#!/usr/bin/env python3
"""
Builds Liftly's bundled exercise seed from free-exercise-db + curation.py.

Usage:
  python3 tools/seed/build_seed.py <path/to/free-exercise-db/exercises.json> [--images]

Writes core/resources/src/commonMain/composeResources/files/exercises.json and, with --images,
downloads + downsizes images (macOS `sips`) to files/exercises/<seedKey>/{0,1}.jpg.
Bump SEED_VERSION in ExerciseSeeder.kt whenever the output changes.
"""
import json, os, re, subprocess, sys, urllib.request

sys.path.insert(0, os.path.dirname(__file__))
from curation import ENTRIES  # noqa: E402

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT_DIR = os.path.join(ROOT, "core/resources/src/commonMain/composeResources/files")
IMG_BASE = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/"

EQUIPMENT = {
    "barbell": "BARBELL", "dumbbell": "DUMBBELL", "machine": "MACHINE", "cable": "CABLE",
    "body only": "BODYWEIGHT", None: "BODYWEIGHT", "kettlebells": "KETTLEBELL", "bands": "BAND",
    "e-z curl bar": "EZ_BAR", "other": "OTHER", "medicine ball": "OTHER", "exercise ball": "OTHER",
    "foam roll": "OTHER",
}
MUSCLES = {"CHEST", "FRONT_DELTS", "SIDE_DELTS", "REAR_DELTS", "BICEPS", "TRICEPS", "FOREARMS", "ABS",
           "OBLIQUES", "LATS", "TRAPS", "UPPER_BACK", "LOWER_BACK", "GLUTES", "QUADS", "HAMSTRINGS",
           "ADDUCTORS", "ABDUCTORS", "CALVES", "NECK"}
TRACKING = {"WEIGHT_REPS", "REPS_ONLY", "TIME", "DISTANCE"}
DROP = re.compile(r"^(repeat|perform|do this for|continue)", re.I)


def clean_instructions(steps):
    out = [s.strip() for s in steps if s.strip() and not DROP.match(s.strip())]
    return out[:6] or steps[:6]


def main():
    src_path = sys.argv[1]
    with_images = "--images" in sys.argv
    source = {e["id"]: e for e in json.load(open(src_path))}
    keys, result, errors = set(), [], []
    for c in ENTRIES:
        s = source.get(c["src"])
        if s is None:
            errors.append(f"unknown source id {c['src']}")
            continue
        if c["key"] in keys:
            errors.append(f"duplicate key {c['key']}")
        keys.add(c["key"])
        for m in [c["primary"], *c["secondary"]]:
            if m not in MUSCLES:
                errors.append(f"{c['key']}: bad muscle {m}")
        if not (3 <= len(c["cues"]) <= 4) or not (2 <= len(c["mistakes"]) <= 4):
            errors.append(f"{c['key']}: needs 3-4 cues and 2-4 mistakes")
        if not (c.get("instructions") or s["instructions"]):
            errors.append(f"{c['key']}: dataset has no instructions; add instructions=[...] in curation.py")
        tracking = c.get("tracking", "WEIGHT_REPS")
        assert tracking in TRACKING
        images = [f"files/exercises/{c['key']}/{i}.jpg" for i in range(min(2, len(s["images"])))]
        result.append({
            "seedKey": c["key"],
            "name": c["name"],
            "aliases": c["aliases"],
            "primaryMuscle": c["primary"],
            "secondaryMuscles": [m for m in c["secondary"] if m != c["primary"]],
            "equipment": c.get("equipment") or EQUIPMENT.get(s.get("equipment"), "OTHER"),
            "trackingType": tracking,
            "mechanic": c.get("mechanic") or (s.get("mechanic") or "").upper() or None,
            "difficulty": {"beginner": "BEGINNER", "intermediate": "INTERMEDIATE", "expert": "EXPERT"}.get(s.get("level")),
            "imagePaths": images,
            "instructions": c.get("instructions") or clean_instructions(s["instructions"]),
            "formCues": c["cues"],
            "commonMistakes": c["mistakes"],
            "source": f"free-exercise-db:{c['src']}",
        })
        if with_images:
            for i, rel in enumerate(images):
                dest = os.path.join(OUT_DIR, rel.removeprefix("files/"))
                if os.path.exists(dest):
                    continue
                os.makedirs(os.path.dirname(dest), exist_ok=True)
                urllib.request.urlretrieve(IMG_BASE + s["images"][i], dest)
                subprocess.run(["sips", "-Z", "360", "-s", "formatOptions", "70", dest], check=True,
                               stdout=subprocess.DEVNULL)
    if errors:
        sys.exit("\n".join(errors))
    result.sort(key=lambda e: e["name"].lower())
    os.makedirs(OUT_DIR, exist_ok=True)
    with open(os.path.join(OUT_DIR, "exercises.json"), "w") as f:
        json.dump({"version": 1, "exercises": result}, f, indent=1, ensure_ascii=False)
    print(f"{len(result)} exercises written")


if __name__ == "__main__":
    main()
