"""
Curated exercise list for Liftly's bundled seed.

Base data (name, instructions, images, level, mechanic) comes from free-exercise-db
(https://github.com/yuhonas/free-exercise-db, public domain / Unlicense).
Aliases, form cues, common mistakes and the finer muscle mapping below are Liftly's own.

Each entry: E(source_id, seed_key, name, aliases, primary, secondary, cues, mistakes, **overrides)
Overrides: equipment=..., tracking=..., mechanic=...
"""

ENTRIES = []


def E(src, key, name, aliases, primary, secondary, cues, mistakes, **kw):
    ENTRIES.append(dict(src=src, key=key, name=name, aliases=aliases, primary=primary,
                        secondary=secondary, cues=cues, mistakes=mistakes, **kw))


# ---------------- Chest ----------------
E("Barbell_Bench_Press_-_Medium_Grip", "bench_press", "Bench Press", ["Barbell Bench Press", "BB Bench", "Flat Bench"],
  "CHEST", ["TRICEPS", "FRONT_DELTS"],
  ["Shoulder blades pinched and down", "Feet planted, slight arch", "Bar to lower chest", "Press up and slightly back"],
  ["Bouncing the bar off the chest", "Elbows flared to 90°", "Butt lifting off the bench"])
E("Barbell_Incline_Bench_Press_-_Medium_Grip", "incline_bench_press", "Incline Bench Press", ["Incline Barbell Press", "Incline Bench"],
  "CHEST", ["FRONT_DELTS", "TRICEPS"],
  ["Bench at 30–45°", "Bar to upper chest", "Elbows slightly tucked"],
  ["Bench set too steep", "Lowering bar to the neck"])
E("Decline_Barbell_Bench_Press", "decline_bench_press", "Decline Bench Press", ["Decline Barbell Press"],
  "CHEST", ["TRICEPS", "FRONT_DELTS"],
  ["Legs locked under pads", "Bar to lower chest", "Controlled descent"],
  ["Using a spotter-less heavy load", "Bouncing off the chest"])
E("Dumbbell_Bench_Press", "db_bench_press", "Dumbbell Bench Press", ["DB Bench", "DB Bench Press", "Dumbbell Press"],
  "CHEST", ["TRICEPS", "FRONT_DELTS"],
  ["Dumbbells over mid-chest", "Lower to chest level", "Press up and slightly in"],
  ["Clanking dumbbells at the top", "Dropping elbows too low"])
E("Incline_Dumbbell_Press", "incline_db_press", "Incline Dumbbell Press", ["Incline DB Press", "Incline DB Bench"],
  "CHEST", ["FRONT_DELTS", "TRICEPS"],
  ["Bench at 30–45°", "Elbows about 45° from torso", "Full stretch at the bottom"],
  ["Bench too upright (turns into shoulder press)", "Half reps"])
E("Decline_Dumbbell_Bench_Press", "decline_db_press", "Decline Dumbbell Press", ["Decline DB Press"],
  "CHEST", ["TRICEPS"],
  ["Secure legs first", "Lower to chest sides", "Squeeze chest at top"],
  ["Rushing the negative", "Wrists bent back"])
E("Dumbbell_Flyes", "db_fly", "Dumbbell Fly", ["DB Fly", "Chest Fly", "Flat Fly"],
  "CHEST", ["FRONT_DELTS"],
  ["Soft bend in elbows", "Open wide like hugging a tree", "Stop at chest level"],
  ["Straightening arms (pressing)", "Going too deep and straining shoulders"])
E("Incline_Dumbbell_Flyes", "incline_db_fly", "Incline Dumbbell Fly", ["Incline DB Fly", "Incline Fly"],
  "CHEST", ["FRONT_DELTS"],
  ["Bench at 30°", "Fixed elbow bend", "Stretch, then squeeze"],
  ["Turning it into a press", "Using too much weight"])
E("Flat_Bench_Cable_Flyes", "cable_fly", "Cable Fly", ["Cable Crossover", "Cable Chest Fly"],
  "CHEST", ["FRONT_DELTS"],
  ["Slight forward lean", "Hands meet in front of chest", "Constant tension"],
  ["Bending elbows more as you go", "Shrugging shoulders"], equipment="CABLE")
E("Butterfly", "pec_deck", "Pec Deck", ["Machine Fly", "Butterfly", "Pec Fly"],
  "CHEST", ["FRONT_DELTS"],
  ["Seat so handles are chest height", "Squeeze for a second", "Slow return"],
  ["Letting weights slam", "Shoulders rolling forward"])
E("Machine_Bench_Press", "machine_chest_press", "Machine Chest Press", ["Chest Press", "Seated Chest Press"],
  "CHEST", ["TRICEPS", "FRONT_DELTS"],
  ["Handles at mid-chest", "Back flat on pad", "Don't lock out hard"],
  ["Seat too high or low", "Shoulders shrugged up"])
E("Smith_Machine_Bench_Press", "smith_bench_press", "Smith Machine Bench Press", ["Smith Bench"],
  "CHEST", ["TRICEPS", "FRONT_DELTS"],
  ["Bench so bar hits lower chest", "Shoulder blades back", "Controlled tempo"],
  ["Bench positioned wrong", "Bouncing off chest"], equipment="SMITH_MACHINE")
E("Pushups", "push_up", "Push-Up", ["Pushup", "Press-Up"],
  "CHEST", ["TRICEPS", "FRONT_DELTS", "ABS"],
  ["Body in one straight line", "Hands under shoulders", "Chest to floor"],
  ["Hips sagging", "Elbows flared wide", "Half reps"], tracking="REPS_ONLY")
E("Dips_-_Chest_Version", "chest_dip", "Chest Dip", ["Dips", "Parallel Bar Dip"],
  "CHEST", ["TRICEPS", "FRONT_DELTS"],
  ["Lean forward", "Elbows out slightly", "Lower until upper arm parallel"],
  ["Going too deep too soon", "Swinging legs"], tracking="REPS_ONLY", equipment="BODYWEIGHT")
E("Straight-Arm_Dumbbell_Pullover", "db_pullover", "Dumbbell Pullover", ["DB Pullover", "Pullover"],
  "CHEST", ["LATS", "TRICEPS"],
  ["Slight elbow bend", "Lower behind head to stretch", "Ribs down"],
  ["Arching the lower back", "Bending elbows into a triceps extension"])
E("Incline_Cable_Chest_Press", "incline_cable_press", "Incline Cable Press", ["Cable Incline Press"],
  "CHEST", ["FRONT_DELTS", "TRICEPS"],
  ["Incline bench between pulleys", "Press up and in", "Control return"],
  ["Letting cables pull shoulders forward", "Short range of motion"])

# ---------------- Shoulders ----------------
E("Standing_Military_Press", "overhead_press", "Overhead Press", ["OHP", "Military Press", "Standing Press", "Barbell Shoulder Press"],
  "FRONT_DELTS", ["SIDE_DELTS", "TRICEPS", "TRAPS"],
  ["Glutes and abs tight", "Bar path straight up", "Head through at the top"],
  ["Leaning back excessively", "Pressing the bar forward around the face"])
E("Seated_Barbell_Military_Press", "seated_barbell_press", "Seated Barbell Press", ["Seated OHP", "Seated Military Press"],
  "FRONT_DELTS", ["SIDE_DELTS", "TRICEPS"],
  ["Back against the pad", "Bar to upper chest", "Lock out overhead"],
  ["Arching off the pad", "Partial reps"])
E("Dumbbell_Shoulder_Press", "db_shoulder_press", "Dumbbell Shoulder Press", ["DB Shoulder Press", "DB OHP", "Seated DB Press"],
  "FRONT_DELTS", ["SIDE_DELTS", "TRICEPS"],
  ["Dumbbells at ear level", "Press up and slightly in", "Elbows a bit forward"],
  ["Lowering too little", "Arching the back"])
E("Arnold_Dumbbell_Press", "arnold_press", "Arnold Press", ["Arnold DB Press"],
  "FRONT_DELTS", ["SIDE_DELTS", "TRICEPS"],
  ["Start palms facing you", "Rotate as you press", "Smooth, controlled turn"],
  ["Rushing the rotation", "Using momentum"])
E("Machine_Shoulder_Military_Press", "machine_shoulder_press", "Machine Shoulder Press", ["Shoulder Press Machine"],
  "FRONT_DELTS", ["SIDE_DELTS", "TRICEPS"],
  ["Handles at shoulder height", "Back flat on pad", "Full lockout"],
  ["Seat too low", "Shrugging up"])
E("Side_Lateral_Raise", "lateral_raise", "Lateral Raise", ["Side Raise", "DB Lateral Raise", "Side Lateral"],
  "SIDE_DELTS", ["TRAPS"],
  ["Lead with elbows", "Raise to shoulder height", "Slight forward lean"],
  ["Swinging the weight", "Shrugging traps", "Raising above shoulders"])
E("Cable_Seated_Lateral_Raise", "cable_lateral_raise", "Cable Lateral Raise", ["Cable Side Raise"],
  "SIDE_DELTS", [],
  ["Cable crosses in front of body", "Raise to shoulder height", "Slow negative"],
  ["Leaning away to cheat", "Bending elbows more and more"])
E("Front_Incline_Dumbbell_Raise", "front_raise", "Front Raise", ["DB Front Raise", "Front Delt Raise"],
  "FRONT_DELTS", ["SIDE_DELTS"],
  ["Arms nearly straight", "Raise to eye level", "No swinging"],
  ["Leaning back", "Going too heavy"])
E("Reverse_Flyes", "reverse_fly", "Reverse Fly", ["Rear Delt Fly", "Bent-Over Reverse Fly", "Rear Delt Raise"],
  "REAR_DELTS", ["UPPER_BACK", "TRAPS"],
  ["Hinge forward, flat back", "Arms out to the side", "Pinch shoulder blades lightly"],
  ["Using the traps instead of rear delts", "Swinging the torso"])
E("Reverse_Machine_Flyes", "reverse_pec_deck", "Reverse Pec Deck", ["Rear Delt Machine", "Reverse Machine Fly"],
  "REAR_DELTS", ["UPPER_BACK"],
  ["Chest against pad", "Arms wide, slight bend", "Pause at the back"],
  ["Shrugging up", "Jerking the handles"])
E("Face_Pull", "face_pull", "Face Pull", ["Rope Face Pull", "Cable Face Pull"],
  "REAR_DELTS", ["UPPER_BACK", "TRAPS"],
  ["Rope at face height", "Pull toward forehead", "Elbows high, rotate out"],
  ["Pulling to the chest", "Leaning back too far"])
E("Upright_Barbell_Row", "upright_row", "Upright Row", ["Barbell Upright Row"],
  "SIDE_DELTS", ["TRAPS", "FRONT_DELTS"],
  ["Shoulder-width grip", "Lead with elbows", "Stop at chest height"],
  ["Pulling too high and pinching shoulders", "Narrow grip"])
E("Barbell_Shrug", "barbell_shrug", "Barbell Shrug", ["Shrugs", "BB Shrug"],
  "TRAPS", ["FOREARMS"],
  ["Shoulders straight up to ears", "Pause at the top", "Arms stay straight"],
  ["Rolling the shoulders", "Bending elbows"])
E("Dumbbell_Shrug", "db_shrug", "Dumbbell Shrug", ["DB Shrug"],
  "TRAPS", ["FOREARMS"],
  ["Dumbbells at sides", "Lift straight up", "Hold one second"],
  ["Using momentum", "Head jutting forward"])
E("Handstand_Push-Ups", "handstand_push_up", "Handstand Push-Up", ["HSPU"],
  "FRONT_DELTS", ["TRICEPS", "TRAPS"],
  ["Hands slightly wider than shoulders", "Head to a tripod position", "Core tight"],
  ["Arching the back", "Elbows flaring wide"], tracking="REPS_ONLY")

# ---------------- Back ----------------
E("Barbell_Deadlift", "deadlift", "Deadlift", ["Conventional Deadlift", "DL", "Barbell Deadlift"],
  "LOWER_BACK", ["GLUTES", "HAMSTRINGS", "QUADS", "TRAPS", "FOREARMS"],
  ["Bar over mid-foot", "Chest up, flat back", "Push the floor away", "Lock out with glutes"],
  ["Rounding the lower back", "Bar drifting away from legs", "Hyperextending at lockout"])
E("Sumo_Deadlift", "sumo_deadlift", "Sumo Deadlift", ["Sumo DL"],
  "HAMSTRINGS", ["GLUTES", "QUADS", "ADDUCTORS", "LOWER_BACK"],
  ["Wide stance, toes out", "Hands inside knees", "Knees track over toes"],
  ["Hips shooting up first", "Knees caving in"])
E("Romanian_Deadlift", "romanian_deadlift", "Romanian Deadlift", ["RDL", "Barbell RDL"],
  "HAMSTRINGS", ["GLUTES", "LOWER_BACK"],
  ["Soft knees, hips back", "Bar slides down thighs", "Stop when hamstrings stretch"],
  ["Rounding the back", "Turning it into a squat"])
E("Stiff-Legged_Dumbbell_Deadlift", "db_romanian_deadlift", "Dumbbell Romanian Deadlift", ["DB RDL", "Dumbbell RDL", "Stiff-Leg DB Deadlift"],
  "HAMSTRINGS", ["GLUTES", "LOWER_BACK"],
  ["Dumbbells close to legs", "Push hips back", "Neutral neck"],
  ["Reaching the floor at all costs", "Rounding shoulders"])
E("Trap_Bar_Deadlift", "trap_bar_deadlift", "Trap Bar Deadlift", ["Hex Bar Deadlift"],
  "QUADS", ["GLUTES", "HAMSTRINGS", "LOWER_BACK", "TRAPS"],
  ["Stand in the center", "Chest up, hips down", "Drive through the floor"],
  ["Rounding the back", "Locking out with a lean back"], equipment="OTHER")
E("Good_Morning", "good_morning", "Good Morning", ["Barbell Good Morning"],
  "HAMSTRINGS", ["LOWER_BACK", "GLUTES"],
  ["Bar on upper back", "Hips back, soft knees", "Flat back throughout"],
  ["Going too heavy", "Rounding the spine"])
E("Bent_Over_Barbell_Row", "barbell_row", "Barbell Row", ["Bent-Over Row", "BB Row", "Pendlay Row"],
  "UPPER_BACK", ["LATS", "REAR_DELTS", "BICEPS"],
  ["Hinge to about 45°", "Pull bar to lower ribs", "Squeeze shoulder blades"],
  ["Standing up during the pull", "Jerking with the lower back"])
E("One-Arm_Dumbbell_Row", "db_row", "Dumbbell Row", ["One-Arm DB Row", "DB Row", "Single-Arm Row"],
  "LATS", ["UPPER_BACK", "REAR_DELTS", "BICEPS"],
  ["Flat back, hand on bench", "Pull elbow to hip", "Full stretch at bottom"],
  ["Twisting the torso", "Shrugging the weight up"])
E("T-Bar_Row_with_Handle", "t_bar_row", "T-Bar Row", ["Landmine Row"],
  "UPPER_BACK", ["LATS", "REAR_DELTS", "BICEPS"],
  ["Chest up, hinge forward", "Pull handle to chest", "Control the lowering"],
  ["Rounding the back", "Using leg drive"])
E("Seated_Cable_Rows", "seated_cable_row", "Seated Cable Row", ["Cable Row", "Low Row"],
  "UPPER_BACK", ["LATS", "BICEPS", "REAR_DELTS"],
  ["Sit tall", "Pull handle to stomach", "Shoulder blades back"],
  ["Rocking the torso", "Shrugging shoulders"])
E("Leverage_Iso_Row", "machine_row", "Machine Row", ["Seated Machine Row", "Iso Row"],
  "UPPER_BACK", ["LATS", "BICEPS"],
  ["Chest on the pad", "Drive elbows back", "Pause and squeeze"],
  ["Leaning off the pad", "Partial reps"])
E("Inverted_Row", "inverted_row", "Inverted Row", ["Bodyweight Row", "Australian Pull-Up"],
  "UPPER_BACK", ["LATS", "BICEPS", "REAR_DELTS"],
  ["Body straight like a plank", "Chest to the bar", "Squeeze at top"],
  ["Hips sagging", "Neck craning forward"], tracking="REPS_ONLY", equipment="BODYWEIGHT")
E("Pullups", "pull_up", "Pull-Up", ["Pullup", "Wide Pull-Up"],
  "LATS", ["BICEPS", "UPPER_BACK"],
  ["Dead hang start", "Chest to the bar", "Drive elbows down"],
  ["Kipping or swinging", "Half reps", "Chin poking forward"], tracking="REPS_ONLY")
E("Chin-Up", "chin_up", "Chin-Up", ["Chinup", "Underhand Pull-Up"],
  "LATS", ["BICEPS", "UPPER_BACK"],
  ["Palms facing you", "Pull chin over bar", "Control the descent"],
  ["Not reaching full extension", "Swinging"], tracking="REPS_ONLY")
E("Wide-Grip_Lat_Pulldown", "lat_pulldown", "Lat Pulldown", ["Pulldown", "Wide-Grip Pulldown"],
  "LATS", ["BICEPS", "UPPER_BACK"],
  ["Thighs locked under pad", "Pull bar to upper chest", "Lean back slightly"],
  ["Pulling behind the neck", "Using body swing"])
E("Close-Grip_Front_Lat_Pulldown", "close_grip_pulldown", "Close-Grip Lat Pulldown", ["Close-Grip Pulldown", "V-Bar Pulldown"],
  "LATS", ["BICEPS", "UPPER_BACK"],
  ["Chest up", "Elbows drive to ribs", "Full stretch at top"],
  ["Leaning way back", "Short range of motion"])
E("Straight-Arm_Pulldown", "straight_arm_pulldown", "Straight-Arm Pulldown", ["Lat Prayer", "Cable Pullover"],
  "LATS", ["TRICEPS"],
  ["Arms nearly straight", "Hinge slightly", "Sweep bar to thighs"],
  ["Bending elbows", "Using the torso to swing"])
E("Hyperextensions_Back_Extensions", "back_extension", "Back Extension", ["Hyperextension", "45° Back Extension"],
  "LOWER_BACK", ["GLUTES", "HAMSTRINGS"],
  ["Pad just below hips", "Hinge at hips", "Stop at a straight line"],
  ["Hyperextending at the top", "Jerking up fast"], tracking="REPS_ONLY")

# ---------------- Arms ----------------
E("Barbell_Curl", "barbell_curl", "Barbell Curl", ["BB Curl", "Standing Curl"],
  "BICEPS", ["FOREARMS"],
  ["Elbows pinned at sides", "Curl to shoulder height", "Slow lowering"],
  ["Swinging the hips", "Elbows drifting forward"])
E("EZ-Bar_Curl", "ez_bar_curl", "EZ-Bar Curl", ["EZ Curl", "EZ Bar Curl"],
  "BICEPS", ["FOREARMS"],
  ["Grip the angled handles", "Elbows still", "Squeeze at top"],
  ["Cheating with momentum", "Cutting the bottom short"])
E("Dumbbell_Bicep_Curl", "db_curl", "Dumbbell Curl", ["DB Curl", "Bicep Curl", "Biceps Curl"],
  "BICEPS", ["FOREARMS"],
  ["Palms forward", "Elbows by your sides", "Full extension at bottom"],
  ["Swinging the dumbbells", "Rushing the negative"])
E("Hammer_Curls", "hammer_curl", "Hammer Curl", ["DB Hammer Curl", "Neutral Grip Curl"],
  "BICEPS", ["FOREARMS"],
  ["Palms facing each other", "Elbows fixed", "Controlled tempo"],
  ["Swinging", "Shoulders rolling forward"])
E("Incline_Dumbbell_Curl", "incline_db_curl", "Incline Dumbbell Curl", ["Incline Curl", "Incline DB Curl"],
  "BICEPS", ["FOREARMS"],
  ["Bench at 45–60°", "Arms hang straight down", "Curl without moving elbows"],
  ["Lifting shoulders off the bench", "Going too heavy"])
E("Preacher_Curl", "preacher_curl", "Preacher Curl", ["Barbell Preacher Curl", "Scott Curl"],
  "BICEPS", ["FOREARMS"],
  ["Armpits snug on pad", "Lower to near straight", "Squeeze at top"],
  ["Dropping the weight fast at the bottom", "Lifting elbows off the pad"])
E("Concentration_Curls", "concentration_curl", "Concentration Curl", ["Seated Concentration Curl"],
  "BICEPS", [],
  ["Elbow against inner thigh", "Curl to shoulder", "Slow negative"],
  ["Using the shoulder", "Moving the upper arm"])
E("Standing_Biceps_Cable_Curl", "cable_curl", "Cable Curl", ["Cable Bicep Curl"],
  "BICEPS", ["FOREARMS"],
  ["Stand close to the pulley", "Elbows fixed", "Constant tension"],
  ["Leaning back", "Elbows moving forward"])
E("Machine_Bicep_Curl", "machine_curl", "Machine Bicep Curl", ["Bicep Curl Machine"],
  "BICEPS", [],
  ["Elbows align with pivot", "Full range", "Controlled return"],
  ["Seat set wrong", "Jerking the handles"])
E("Spider_Curl", "spider_curl", "Spider Curl", ["Prone Incline Curl"],
  "BICEPS", [],
  ["Chest on incline bench", "Arms hang vertical", "Squeeze hard at top"],
  ["Swinging the arms", "Partial reps"])
E("Reverse_Barbell_Curl", "reverse_curl", "Reverse Curl", ["Reverse Grip Curl"],
  "FOREARMS", ["BICEPS"],
  ["Overhand grip", "Wrists straight", "Elbows still"],
  ["Wrists bending back", "Too much weight"])
E("Palms-Up_Barbell_Wrist_Curl_Over_A_Bench", "wrist_curl", "Wrist Curl", ["Barbell Wrist Curl"],
  "FOREARMS", [],
  ["Forearms on bench", "Let bar roll to fingers", "Curl wrists up"],
  ["Lifting forearms off bench", "Bouncing"])
E("Close-Grip_Barbell_Bench_Press", "close_grip_bench", "Close-Grip Bench Press", ["CGBP", "Close Grip Bench"],
  "TRICEPS", ["CHEST", "FRONT_DELTS"],
  ["Hands shoulder-width", "Elbows tucked", "Bar to lower chest"],
  ["Grip too narrow (wrist strain)", "Elbows flaring"])
E("EZ-Bar_Skullcrusher", "skull_crusher", "Skull Crusher", ["Lying Triceps Extension", "EZ Bar Skullcrusher"],
  "TRICEPS", [],
  ["Upper arms angled slightly back", "Lower bar toward forehead", "Only elbows move"],
  ["Elbows flaring out", "Turning it into a press"])
E("Triceps_Pushdown", "triceps_pushdown", "Triceps Pushdown", ["Cable Pushdown", "Tricep Pushdown"],
  "TRICEPS", [],
  ["Elbows pinned to sides", "Push to full lockout", "Slow return"],
  ["Leaning over the bar", "Elbows moving forward"])
E("Triceps_Pushdown_-_Rope_Attachment", "rope_pushdown", "Rope Pushdown", ["Rope Triceps Pushdown", "Rope Tricep Extension"],
  "TRICEPS", [],
  ["Elbows still", "Spread rope at bottom", "Squeeze triceps"],
  ["Using body weight", "Partial lockout"])
E("Cable_Rope_Overhead_Triceps_Extension", "overhead_cable_extension", "Overhead Cable Triceps Extension", ["Overhead Rope Extension"],
  "TRICEPS", [],
  ["Face away from pulley", "Elbows by ears", "Extend fully overhead"],
  ["Elbows flaring", "Arching the back"])
E("Standing_Dumbbell_Triceps_Extension", "db_overhead_extension", "Dumbbell Overhead Extension", ["Overhead DB Extension", "French Press"],
  "TRICEPS", [],
  ["Hold one dumbbell with both hands", "Lower behind head", "Elbows point up"],
  ["Elbows wide", "Arching the lower back"])
E("Tricep_Dumbbell_Kickback", "triceps_kickback", "Triceps Kickback", ["DB Kickback", "Tricep Kickback"],
  "TRICEPS", [],
  ["Upper arm parallel to floor", "Extend to straight arm", "Pause at top"],
  ["Swinging the arm", "Dropping the elbow"])
E("Dips_-_Triceps_Version", "triceps_dip", "Triceps Dip", ["Dips", "Bar Dip"],
  "TRICEPS", ["CHEST", "FRONT_DELTS"],
  ["Body upright", "Elbows back, not out", "Lower to 90° elbows"],
  ["Shrugging shoulders", "Going too deep"], tracking="REPS_ONLY", equipment="BODYWEIGHT")
E("Bench_Dips", "bench_dip", "Bench Dip", ["Chair Dip"],
  "TRICEPS", ["CHEST", "FRONT_DELTS"],
  ["Hands on bench edge", "Hips close to bench", "Lower to 90°"],
  ["Shoulders rolling forward", "Going too low"], tracking="REPS_ONLY")

# ---------------- Legs ----------------
E("Barbell_Squat", "back_squat", "Back Squat", ["Squat", "Barbell Squat", "High-Bar Squat"],
  "QUADS", ["GLUTES", "HAMSTRINGS", "LOWER_BACK"],
  ["Bar on upper back", "Brace before descending", "Knees track over toes", "Hips below knees if mobile"],
  ["Knees caving in", "Heels lifting", "Chest collapsing forward"])
E("Front_Barbell_Squat", "front_squat", "Front Squat", ["Barbell Front Squat"],
  "QUADS", ["GLUTES", "ABS"],
  ["Elbows high", "Torso upright", "Sit straight down"],
  ["Elbows dropping", "Rounding upper back"])
E("Goblet_Squat", "goblet_squat", "Goblet Squat", ["DB Goblet Squat", "KB Goblet Squat"],
  "QUADS", ["GLUTES", "ABS"],
  ["Weight held at chest", "Elbows inside knees", "Sit between heels"],
  ["Heels rising", "Leaning forward"], equipment="DUMBBELL")
E("Smith_Machine_Squat", "smith_squat", "Smith Machine Squat", ["Smith Squat"],
  "QUADS", ["GLUTES", "HAMSTRINGS"],
  ["Feet slightly forward", "Controlled depth", "Drive up through heels"],
  ["Feet directly under bar", "Bouncing at bottom"], equipment="SMITH_MACHINE")
E("Hack_Squat", "hack_squat", "Hack Squat", ["Machine Hack Squat"],
  "QUADS", ["GLUTES"],
  ["Back flat on pad", "Feet shoulder-width", "Go deep, controlled"],
  ["Knees caving", "Half reps"])
E("Leg_Press", "leg_press", "Leg Press", ["45° Leg Press", "Machine Leg Press"],
  "QUADS", ["GLUTES", "HAMSTRINGS"],
  ["Feet mid-platform", "Lower until knees ~90°", "Don't lock knees"],
  ["Lower back lifting off the pad", "Locking out knees hard"])
E("Leg_Extensions", "leg_extension", "Leg Extension", ["Quad Extension"],
  "QUADS", [],
  ["Knees aligned with pivot", "Extend fully", "Slow lowering"],
  ["Swinging the weight", "Lifting hips off seat"])
E("Lying_Leg_Curls", "lying_leg_curl", "Lying Leg Curl", ["Leg Curl", "Hamstring Curl"],
  "HAMSTRINGS", ["CALVES"],
  ["Hips pressed into pad", "Curl heels to glutes", "Slow negative"],
  ["Hips lifting", "Partial range"])
E("Seated_Leg_Curl", "seated_leg_curl", "Seated Leg Curl", ["Seated Hamstring Curl"],
  "HAMSTRINGS", [],
  ["Thigh pad snug", "Curl fully", "Control return"],
  ["Leaning forward", "Rushing reps"])
E("Barbell_Lunge", "barbell_lunge", "Barbell Lunge", ["BB Lunge"],
  "QUADS", ["GLUTES", "HAMSTRINGS"],
  ["Long step", "Back knee toward floor", "Torso upright"],
  ["Front knee caving", "Short steps"])
E("Dumbbell_Lunges", "db_lunge", "Dumbbell Lunge", ["DB Lunge", "Lunges"],
  "QUADS", ["GLUTES", "HAMSTRINGS"],
  ["Step forward, drop straight down", "Front shin vertical", "Push back through heel"],
  ["Leaning forward", "Knee hitting the floor hard"])
E("Bodyweight_Walking_Lunge", "walking_lunge", "Walking Lunge", ["Lunge Walk"],
  "QUADS", ["GLUTES", "HAMSTRINGS"],
  ["Upright torso", "Alternate legs smoothly", "Knee just above floor"],
  ["Wobbling side to side", "Short strides"], tracking="REPS_ONLY", equipment="BODYWEIGHT")
E("Split_Squat_with_Dumbbells", "bulgarian_split_squat", "Bulgarian Split Squat", ["BSS", "Rear-Foot Elevated Split Squat", "Split Squat"],
  "QUADS", ["GLUTES", "HAMSTRINGS"],
  ["Rear foot on bench", "Drop straight down", "Front heel stays planted"],
  ["Front foot too close", "Pushing off back foot"])
E("Dumbbell_Step_Ups", "step_up", "Step-Up", ["DB Step-Up", "Box Step-Up"],
  "QUADS", ["GLUTES"],
  ["Whole foot on box", "Drive through front heel", "Control the way down"],
  ["Pushing off the back leg", "Box too high"])
E("Barbell_Hip_Thrust", "hip_thrust", "Hip Thrust", ["Barbell Hip Thrust", "Glute Thrust"],
  "GLUTES", ["HAMSTRINGS"],
  ["Upper back on bench edge", "Chin tucked", "Squeeze glutes at top"],
  ["Overarching the lower back", "Feet too far forward"])
E("Barbell_Glute_Bridge", "glute_bridge", "Glute Bridge", ["Barbell Glute Bridge", "Hip Bridge"],
  "GLUTES", ["HAMSTRINGS"],
  ["Feet flat, close to hips", "Drive through heels", "Pause at top"],
  ["Pushing with toes", "Arching instead of squeezing"])
E("One-Legged_Cable_Kickback", "cable_kickback", "Cable Glute Kickback", ["Glute Kickback", "Cable Kickback"],
  "GLUTES", ["HAMSTRINGS"],
  ["Slight forward lean", "Kick back and up", "Don't arch the back"],
  ["Swinging the leg", "Rotating the hips"])
E("Thigh_Adductor", "hip_adduction", "Hip Adduction Machine", ["Adductor Machine", "Inner Thigh Machine"],
  "ADDUCTORS", [],
  ["Sit tall", "Squeeze legs together", "Slow return"],
  ["Using momentum", "Too much range too soon"])
E("Thigh_Abductor", "hip_abduction", "Hip Abduction Machine", ["Abductor Machine", "Outer Thigh Machine"],
  "ABDUCTORS", ["GLUTES"],
  ["Back against pad", "Push knees out", "Controlled return"],
  ["Letting the stack slam", "Leaning forward"])
E("Standing_Calf_Raises", "standing_calf_raise", "Standing Calf Raise", ["Calf Raise", "Machine Calf Raise"],
  "CALVES", [],
  ["Balls of feet on edge", "Full stretch at bottom", "Pause at top"],
  ["Bouncing", "Bending knees"])
E("Seated_Calf_Raise", "seated_calf_raise", "Seated Calf Raise", ["Seated Calves"],
  "CALVES", [],
  ["Pad on lower thighs", "Deep stretch", "Squeeze at top"],
  ["Short bouncy reps", "Rocking the torso"])
E("Calf_Press_On_The_Leg_Press_Machine", "leg_press_calf_raise", "Leg Press Calf Raise", ["Calf Press"],
  "CALVES", [],
  ["Balls of feet on platform edge", "Knees softly locked", "Full range"],
  ["Feet slipping", "Bending knees"])
E("Box_Jump_Multiple_Response", "box_jump", "Box Jump", ["Box Jumps"],
  "QUADS", ["GLUTES", "CALVES", "HAMSTRINGS"],
  ["Swing arms", "Land softly, knees bent", "Step down"],
  ["Box too high", "Jumping down"], tracking="REPS_ONLY", equipment="BODYWEIGHT")
E("Bodyweight_Squat", "bodyweight_squat", "Bodyweight Squat", ["Air Squat"],
  "QUADS", ["GLUTES", "HAMSTRINGS"],
  ["Feet shoulder-width", "Arms forward for balance", "Hips below knees"],
  ["Heels lifting", "Knees caving"], tracking="REPS_ONLY")

# ---------------- Core ----------------
E("Plank", "plank", "Plank", ["Front Plank", "Forearm Plank"],
  "ABS", ["OBLIQUES", "LOWER_BACK"],
  ["Elbows under shoulders", "Squeeze glutes", "Straight line head to heels"],
  ["Hips sagging", "Hips piked too high", "Holding breath"], tracking="TIME")
E("Crunches", "crunch", "Crunch", ["Crunches", "Ab Crunch"],
  "ABS", [],
  ["Lower back on floor", "Curl ribs toward hips", "Exhale at top"],
  ["Pulling on the neck", "Sitting all the way up"], tracking="REPS_ONLY")
E("Cable_Crunch", "cable_crunch", "Cable Crunch", ["Kneeling Cable Crunch", "Rope Crunch"],
  "ABS", [],
  ["Kneel, rope by ears", "Curl elbows to knees", "Hips stay still"],
  ["Pulling with arms", "Sitting back on heels"])
E("Ab_Crunch_Machine", "ab_crunch_machine", "Ab Crunch Machine", ["Machine Crunch"],
  "ABS", [],
  ["Feet secured", "Crunch, don't pull", "Slow return"],
  ["Using arms to pull", "Going too heavy"])
E("Hanging_Leg_Raise", "hanging_leg_raise", "Hanging Leg Raise", ["Leg Raise", "Hanging Knee Raise"],
  "ABS", ["OBLIQUES"],
  ["Dead hang, shoulders active", "Curl pelvis up", "Lower slowly"],
  ["Swinging", "Only lifting legs, not pelvis"], tracking="REPS_ONLY")
E("Flat_Bench_Lying_Leg_Raise", "lying_leg_raise", "Lying Leg Raise", ["Bench Leg Raise"],
  "ABS", [],
  ["Lower back pressed down", "Legs straight", "Lower without touching"],
  ["Arching the back", "Dropping legs fast"], tracking="REPS_ONLY")
E("Reverse_Crunch", "reverse_crunch", "Reverse Crunch", [],
  "ABS", [],
  ["Knees bent at 90°", "Roll hips off floor", "Control down"],
  ["Swinging legs", "Using momentum"], tracking="REPS_ONLY")
E("Russian_Twist", "russian_twist", "Russian Twist", ["Seated Twist"],
  "OBLIQUES", ["ABS"],
  ["Lean back slightly", "Rotate from the torso", "Feet up for harder version"],
  ["Only moving arms", "Rounding the back"], tracking="REPS_ONLY")
E("Ab_Roller", "ab_wheel", "Ab Wheel Rollout", ["Ab Roller", "Ab Rollout"],
  "ABS", ["LATS", "LOWER_BACK"],
  ["Start on knees", "Roll out with a flat back", "Pull back with abs"],
  ["Lower back sagging", "Going too far too soon"], tracking="REPS_ONLY")
E("Pallof_Press", "pallof_press", "Pallof Press", ["Anti-Rotation Press"],
  "OBLIQUES", ["ABS"],
  ["Side-on to the cable", "Press straight out", "Resist the rotation"],
  ["Twisting toward the cable", "Standing too close"])
E("Standing_Cable_Wood_Chop", "wood_chop", "Cable Wood Chop", ["Wood Chop", "Cable Woodchopper"],
  "OBLIQUES", ["ABS"],
  ["Rotate through the torso", "Pivot back foot", "Arms stay long"],
  ["Pulling with arms only", "Rounding the spine"])
E("Sit-Up", "sit_up", "Sit-Up", ["Situp"],
  "ABS", [],
  ["Feet anchored", "Roll up smoothly", "Control the descent"],
  ["Yanking the neck", "Bouncing off the floor"], tracking="REPS_ONLY")
E("Mountain_Climbers", "mountain_climber", "Mountain Climber", ["Mountain Climbers"],
  "ABS", ["QUADS", "FRONT_DELTS"],
  ["Plank position", "Drive knees to chest", "Hips level"],
  ["Hips bouncing high", "Hands too far forward"], tracking="TIME", equipment="BODYWEIGHT")

# ---------------- Full body / kettlebell ----------------
E("One-Arm_Kettlebell_Swings", "kettlebell_swing", "Kettlebell Swing", ["KB Swing", "Russian Swing"],
  "GLUTES", ["HAMSTRINGS", "LOWER_BACK", "FRONT_DELTS"],
  ["Hinge, don't squat", "Snap hips forward", "Arms just guide the bell"],
  ["Lifting with the arms", "Rounding the back"],
  instructions=[
      "Stand with feet slightly wider than shoulders, kettlebell on the floor a foot in front of you.",
      "Hinge at the hips with a flat back and grab the handle with both hands.",
      "Hike the bell back between your legs like a football snap.",
      "Drive your hips forward explosively so the bell floats up to chest height; arms stay relaxed.",
      "Let the bell fall back between your legs, hinging at the hips to absorb it, and go straight into the next rep.",
  ])
E("Kettlebell_Thruster", "thruster", "Thruster", ["KB Thruster", "Squat to Press"],
  "QUADS", ["FRONT_DELTS", "GLUTES", "TRICEPS"],
  ["Front squat down", "Drive up explosively", "Press overhead in one motion"],
  ["Pausing between squat and press", "Knees caving"])
E("Power_Clean", "power_clean", "Power Clean", ["Clean"],
  "HAMSTRINGS", ["GLUTES", "QUADS", "TRAPS", "LOWER_BACK"],
  ["Bar close to body", "Explode through hips", "Fast elbows to rack"],
  ["Pulling with arms early", "Bar swinging away"])
E("Farmers_Walk", "farmers_walk", "Farmer's Walk", ["Farmer Carry", "Farmers Carry"],
  "FOREARMS", ["TRAPS", "ABS"],
  ["Stand tall", "Shoulders back", "Short, quick steps"],
  ["Leaning to one side", "Shrugging the weights"], tracking="DISTANCE")

# ---------------- Cardio ----------------
E("Running_Treadmill", "treadmill_run", "Treadmill Run", ["Running", "Treadmill"],
  "QUADS", ["HAMSTRINGS", "CALVES"],
  ["Upright posture", "Land under your hips", "Relaxed shoulders"],
  ["Overstriding", "Holding the handrails"], tracking="DISTANCE")
E("Rowing_Stationary", "rowing_machine", "Rowing Machine", ["Rower", "Erg", "Indoor Row"],
  "UPPER_BACK", ["QUADS", "LATS", "BICEPS"],
  ["Legs, then body, then arms", "Reverse on the return", "Strong leg drive"],
  ["Pulling with arms first", "Rounding the back"], tracking="DISTANCE")
E("Bicycling_Stationary", "stationary_bike", "Stationary Bike", ["Exercise Bike", "Spin Bike", "Cycling"],
  "QUADS", ["HAMSTRINGS", "CALVES"],
  ["Seat at hip height", "Slight knee bend at bottom", "Steady cadence"],
  ["Seat too low", "Bouncing on the saddle"], tracking="TIME")
E("Rope_Jumping", "jump_rope", "Jump Rope", ["Skipping", "Skipping Rope"],
  "CALVES", ["QUADS", "FRONT_DELTS"],
  ["Small jumps on balls of feet", "Turn rope with wrists", "Elbows close"],
  ["Jumping too high", "Swinging arms wide"], tracking="TIME", equipment="OTHER")

# ---------------- Extra machines / variations ----------------
E("Smith_Machine_Incline_Bench_Press", "smith_incline_press", "Smith Machine Incline Press", ["Smith Incline"],
  "CHEST", ["FRONT_DELTS", "TRICEPS"],
  ["Bench at 30°", "Bar to upper chest", "Shoulder blades set"],
  ["Bench too steep", "Bouncing the bar"], equipment="SMITH_MACHINE")
E("Leverage_Incline_Chest_Press", "incline_machine_press", "Incline Machine Press", ["Incline Chest Press Machine"],
  "CHEST", ["FRONT_DELTS", "TRICEPS"],
  ["Handles at upper chest", "Back on pad", "Controlled return"],
  ["Shoulders rolling forward", "Partial reps"])
E("Leverage_Shoulder_Press", "lever_shoulder_press", "Lever Shoulder Press", ["Hammer Strength Shoulder Press"],
  "FRONT_DELTS", ["SIDE_DELTS", "TRICEPS"],
  ["Handles at shoulder level", "Press to lockout", "Core braced"],
  ["Arching off the pad", "Seat too low"])
E("Leverage_High_Row", "high_row", "High Row Machine", ["Hammer Strength High Row"],
  "LATS", ["UPPER_BACK", "BICEPS"],
  ["Chest against pad", "Pull down and back", "Full stretch"],
  ["Shrugging", "Leaning back"])
E("Seated_One-arm_Cable_Pulley_Rows", "single_arm_cable_row", "Single-Arm Cable Row", ["One-Arm Cable Row"],
  "LATS", ["UPPER_BACK", "BICEPS"],
  ["Sit tall", "Pull elbow past torso", "Allow slight rotation at stretch"],
  ["Twisting excessively", "Shrugging"])
E("Smith_Machine_Calf_Raise", "smith_calf_raise", "Smith Machine Calf Raise", ["Smith Calf Raise"],
  "CALVES", [],
  ["Toes on a plate", "Full stretch", "Pause at top"],
  ["Bouncing", "Short range"], equipment="SMITH_MACHINE")
E("Standing_Dumbbell_Calf_Raise", "db_calf_raise", "Dumbbell Calf Raise", ["DB Calf Raise"],
  "CALVES", [],
  ["Hold dumbbells at sides", "Rise onto toes", "Slow lowering"],
  ["Bouncing", "Leaning forward"])
E("Smith_Machine_Stiff-Legged_Deadlift", "smith_rdl", "Smith Machine Romanian Deadlift", ["Smith RDL"],
  "HAMSTRINGS", ["GLUTES", "LOWER_BACK"],
  ["Soft knees", "Hips back", "Bar stays close"],
  ["Rounding the back", "Squatting the weight"], equipment="SMITH_MACHINE")
E("Kettlebell_One-Legged_Deadlift", "single_leg_rdl", "Single-Leg Romanian Deadlift", ["Single-Leg RDL", "SL RDL"],
  "HAMSTRINGS", ["GLUTES", "LOWER_BACK"],
  ["Hips square to floor", "Back leg in line with torso", "Slow and balanced"],
  ["Opening the hip", "Rounding the back"])
E("Zercher_Squats", "zercher_squat", "Zercher Squat", [],
  "QUADS", ["GLUTES", "ABS", "UPPER_BACK"],
  ["Bar in elbow crooks", "Upright torso", "Brace hard"],
  ["Rounding upper back", "Elbows dropping"])
E("Overhead_Squat", "overhead_squat", "Overhead Squat", ["OHS"],
  "QUADS", ["GLUTES", "FRONT_DELTS", "ABS"],
  ["Wide grip, arms locked", "Bar over mid-foot", "Upright torso"],
  ["Bar drifting forward", "Elbows bending"])
E("Zottman_Curl", "zottman_curl", "Zottman Curl", [],
  "BICEPS", ["FOREARMS"],
  ["Curl palms up", "Rotate palms down at top", "Lower slowly"],
  ["Rushing the rotation", "Swinging"])
E("Cable_Rear_Delt_Fly", "cable_rear_delt_fly", "Cable Rear Delt Fly", ["Cable Reverse Fly"],
  "REAR_DELTS", ["UPPER_BACK"],
  ["Cross the cables", "Pull arms wide", "Slight elbow bend"],
  ["Shrugging", "Leaning back"])
E("Smith_Machine_Overhead_Shoulder_Press", "smith_shoulder_press", "Smith Machine Shoulder Press", ["Smith OHP"],
  "FRONT_DELTS", ["SIDE_DELTS", "TRICEPS"],
  ["Bench just behind bar path", "Bar to chin level", "Full lockout"],
  ["Lowering behind the neck", "Arching"], equipment="SMITH_MACHINE")
