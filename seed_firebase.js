/**
 * SafeWalk – Firestore Database Seed Script
 * ─────────────────────────────────────────
 * HOW TO USE:
 *   1. Go to Firebase Console → Project Settings → Service Accounts
 *   2. Click "Generate new private key" → download the JSON file
 *   3. Replace "serviceAccountKey.json" below with the path to that file
 *   4. Run:  node seed_firestore.js
 *
 * This script creates all collections with sample documents that match
 * the exact field names used in your Java source files.
 */

const admin = require("firebase-admin");

// ── CHANGE THIS to your downloaded service account key path ──
const serviceAccount = require("safewalk-775e9-firebase-adminsdk-fbsvc-a627f5aa35.json");

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
});

const db = admin.firestore();

// ─────────────────────────────────────────────────────────────
// HELPER
// ─────────────────────────────────────────────────────────────
async function set(path, data) {
  const parts = path.split("/");
  let ref;
  if (parts.length % 2 === 0) {
    // document path (even segments)
    ref = db.doc(path);
  } else {
    // collection path – shouldn't happen, guard anyway
    throw new Error("set() requires a document path: " + path);
  }
  await ref.set(data, { merge: true });
  console.log("  ✓  " + path);
}

// ─────────────────────────────────────────────────────────────
// SAMPLE DATA
// ─────────────────────────────────────────────────────────────

const SAMPLE_UID       = "user_sample_001";
const SAMPLE_JOURNEY_1 = "journey_sample_001";
const SAMPLE_JOURNEY_2 = "journey_sample_002";
const SAMPLE_ALERT_1   = "alert_sample_001";

async function seed() {
  console.log("\n🔥  SafeWalk – Seeding Firestore…\n");

  // ── 1. USERS ──────────────────────────────────────────────
  // Used in: ActiveWalkActivity, StartWalk, WalkHistoryActivity
  console.log("📁  users");
  await set(`users/${SAMPLE_UID}`, {
    uid:       SAMPLE_UID,
    name:      "Janel Santos",
    email:     "janel@example.com",
    phone:     "+639171234567",
    createdAt: Date.now(),
  });

  // ── 2. USERS / GUARDIANS (sub-collection) ─────────────────
  // Used in: ActiveWalkActivity (loads guardian phones for SMS)
  console.log("\n📁  users/{uid}/guardians");
  await set(`users/${SAMPLE_UID}/guardians/guardian_001`, {
    guardianId: "guardian_001",
    name:       "Mom",
    phone:      "+639179998888",
    addedAt:    Date.now(),
  });
  await set(`users/${SAMPLE_UID}/guardians/guardian_002`, {
    guardianId: "guardian_002",
    name:       "Ryan (Roommate)",
    phone:      "+639171112222",
    addedAt:    Date.now(),
  });

  // ── 3. JOURNEYS ───────────────────────────────────────────
  // Used in: StartWalk (create), ActiveWalkActivity (update),
  //          WalkHistoryActivity (read), JourneyData.java (model)
  console.log("\n📁  journeys");
  await set(`journeys/${SAMPLE_JOURNEY_1}`, {
    journeyId:       SAMPLE_JOURNEY_1,
    userId:          SAMPLE_UID,
    destination:     "University of the Philippines, Diliman",
    checkinInterval: "15 minutes",
    arrivalTime:     "18:30",
    startLat:        14.6507,
    startLng:        121.0490,
    endLat:          14.6548,
    endLng:          121.0682,
    startTime:       Date.now() - 3600000,   // 1 hour ago
    endTime:         Date.now() - 1800000,   // 30 min ago
    distanceMeters:  2340,
    durationMinutes: 30,
    status:          "completed",            // "active" | "completed" | "panic"
    alertsTriggered: 0,
  });

  await set(`journeys/${SAMPLE_JOURNEY_2}`, {
    journeyId:       SAMPLE_JOURNEY_2,
    userId:          SAMPLE_UID,
    destination:     "Bonifacio Global City, Taguig",
    checkinInterval: "10 minutes",
    arrivalTime:     "22:00",
    startLat:        14.5507,
    startLng:        121.0490,
    endLat:          0,
    endLng:          0,
    startTime:       Date.now() - 86400000,  // yesterday
    endTime:         Date.now() - 82800000,
    distanceMeters:  1800,
    durationMinutes: 45,
    status:          "completed",
    alertsTriggered: 1,                      // had 1 panic event
  });

  // ── 4. LOCATIONS ──────────────────────────────────────────
  // Used in: ActiveWalkActivity.updateFirestoreLocation()
  // One document per user – overwritten on every location update
  console.log("\n📁  locations");
  await set(`locations/${SAMPLE_UID}`, {
    userId:    SAMPLE_UID,
    journeyId: SAMPLE_JOURNEY_1,
    lat:       14.6548,
    lng:       121.0682,
    timestamp: Date.now(),
  });

  // ── 5. ALERTS ─────────────────────────────────────────────
  // Used in: ActiveWalkActivity.sendPanicAlert()
  console.log("\n📁  alerts");
  await set(`alerts/${SAMPLE_ALERT_1}`, {
    alertId:   SAMPLE_ALERT_1,
    userId:    SAMPLE_UID,
    journeyId: SAMPLE_JOURNEY_2,
    type:      "PANIC",          // always "PANIC" in current code
    lat:       14.5510,
    lng:       121.0495,
    timestamp: Date.now() - 82900000,
  });

  console.log("\n✅  All collections seeded successfully!\n");
  console.log("Collections created:");
  console.log("  • users");
  console.log("  • users/{uid}/guardians");
  console.log("  • journeys");
  console.log("  • locations");
  console.log("  • alerts");
  console.log("\nOpen Firebase Console → Firestore Database to view your data.");
}

seed().catch((err) => {
  console.error("\n❌  Seed failed:", err.message);
  process.exit(1);
});
