const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_user_123";
const BOB_UID = "bob_user_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write items or user_settings", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("items").get());
  await assertFails(
    unauthDb.collection("items").doc("item_1").set({
      ownerId: ALICE_UID,
      name: "鲜牛奶",
      category: "food",
      expiry_date: "2026-11-20",
      used_up: false,
      created_at: ts,
      updated_at: ts,
    })
  );
  await assertFails(unauthDb.collection("user_settings").doc(ALICE_UID).get());
});

test("Authenticated owner: Kotlin-mirrored item create, toggle used_up, edit, and delete succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const createdTs = new Date(Date.now() - 10000);

  // Create without optional note (Kotlin omits null note via filterValues)
  await assertSucceeds(
    aliceDb.collection("items").doc("item_milk").set({
      ownerId: ALICE_UID,
      name: "鲜牛奶",
      category: "food",
      expiry_date: "2026-11-20",
      used_up: false,
      created_at: createdTs,
      updated_at: createdTs,
    })
  );

  // Create with optional non-empty note
  await assertSucceeds(
    aliceDb.collection("items").doc("item_med").set({
      ownerId: ALICE_UID,
      name: "布洛芬",
      category: "medicine",
      expiry_date: "2027-05-01",
      note: "餐后服用",
      used_up: false,
      created_at: createdTs,
      updated_at: createdTs,
    })
  );

  // Partial update: toggle used_up
  await assertSucceeds(
    aliceDb.collection("items").doc("item_milk").update({
      used_up: true,
      updated_at: new Date(Date.now() - 2000),
    })
  );

  // Partial update: edit details + add note
  await assertSucceeds(
    aliceDb.collection("items").doc("item_milk").update({
      name: "有机鲜牛奶",
      category: "food",
      expiry_date: "2026-11-22",
      note: "冷藏上层",
      used_up: false,
      updated_at: new Date(Date.now() - 1000),
    })
  );

  // Delete own item
  await assertSucceeds(aliceDb.collection("items").doc("item_milk").delete());
});

test("Security guards: reject ghost fields, extra id field, future timestamps, invalid category, and immutable mutations", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const createdTs = new Date(Date.now() - 10000);

  // Reject extra 'id' field in payload
  await assertFails(
    aliceDb.collection("items").doc("bad_id").set({
      id: "bad_id",
      ownerId: ALICE_UID,
      name: "酸奶",
      category: "food",
      expiry_date: "2026-11-20",
      used_up: false,
      created_at: createdTs,
      updated_at: createdTs,
    })
  );

  // Reject invalid category
  await assertFails(
    aliceDb.collection("items").doc("bad_cat").set({
      ownerId: ALICE_UID,
      name: "酸奶",
      category: "electronics",
      expiry_date: "2026-11-20",
      used_up: false,
      created_at: createdTs,
      updated_at: createdTs,
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("items").doc("bad_future").set({
      ownerId: ALICE_UID,
      name: "酸奶",
      category: "food",
      expiry_date: "2026-11-20",
      used_up: false,
      created_at: new Date(Date.now() + 120000),
      updated_at: new Date(Date.now() + 120000),
    })
  );

  // Seed valid item, then test illegal updates
  await assertSucceeds(
    aliceDb.collection("items").doc("valid_1").set({
      ownerId: ALICE_UID,
      name: "面包",
      category: "food",
      expiry_date: "2026-11-19",
      used_up: false,
      created_at: createdTs,
      updated_at: createdTs,
    })
  );

  // Reject ghost field on update
  await assertFails(
    aliceDb.collection("items").doc("valid_1").update({
      used_up: true,
      isVerified: true,
      updated_at: new Date(Date.now() - 1000),
    })
  );

  // Reject mutating created_at
  await assertFails(
    aliceDb.collection("items").doc("valid_1").update({
      created_at: new Date(Date.now() - 500),
      updated_at: new Date(Date.now() - 500),
    })
  );
});

test("Cross-user isolation & client query alignment on items and user_settings", async () => {
  const createdTs = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("items").doc("alice_item").set({
      ownerId: ALICE_UID,
      name: "牛奶",
      category: "food",
      expiry_date: "2026-11-20",
      used_up: false,
      created_at: createdTs,
      updated_at: createdTs,
    });
    await context.firestore().collection("items").doc("bob_item").set({
      ownerId: BOB_UID,
      name: "眼药水",
      category: "medicine",
      expiry_date: "2026-12-01",
      used_up: false,
      created_at: createdTs,
      updated_at: createdTs,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Filtered query matching Kotlin repository .whereEqualTo("ownerId", uid) succeeds
  await assertSucceeds(
    aliceDb.collection("items").where("ownerId", "==", ALICE_UID).get()
  );

  // Unfiltered top-level query fails
  await assertFails(aliceDb.collection("items").get());

  // Cross-user read/update/delete fails
  await assertFails(aliceDb.collection("items").doc("bob_item").get());
  await assertFails(
    aliceDb.collection("items").doc("bob_item").update({
      used_up: true,
      updated_at: new Date(Date.now() - 1000),
    })
  );
  await assertFails(aliceDb.collection("items").doc("bob_item").delete());

  // User settings isolation
  await assertSucceeds(
    aliceDb.collection("user_settings").doc(ALICE_UID).set({
      ownerId: ALICE_UID,
      remindersEnabled: true,
      reminderHour: 9,
      reminderMinute: 0,
      daysBefore: 3,
      language: "zh",
      createdAt: createdTs,
      updatedAt: createdTs,
    })
  );
  await assertFails(aliceDb.collection("user_settings").doc(BOB_UID).get());
});
