package com.biowellness.app.utils

/**
 * A small curated library of common bodyweight / light-equipment
 * exercises. Content follows widely established strength & conditioning
 * form cues (the kind of guidance found in ACE / NASM / ACSM material).
 * This is general fitness education, not personalised medical or
 * physical-therapy advice.
 */

enum class ExerciseCategory { LEGS, CHEST, BACK, ARMS, SHOULDERS, CORE, CARDIO, FULL_BODY }
enum class Difficulty { BEGINNER, INTERMEDIATE, ADVANCED }
enum class Equipment { NONE, DUMBBELLS }

enum class PoseKey {
    STANDING, ARMS_DOWN, ARMS_UP_OUT, SQUAT_DOWN, LUNGE_FORWARD,
    PLANK, PUSHUP_DOWN, LYING_FLAT, BRIDGE_UP, SITUP_UP,
    HINGE_DEADLIFT, ARM_CURL_UP, SHOULDER_PRESS_UP, MOUNTAIN_CLIMBER
}

data class Exercise(
    val id: String,
    val name: String,
    val category: ExerciseCategory,
    val equipment: Equipment,
    val difficulty: Difficulty,
    val primaryMuscles: List<String>,
    val startPose: PoseKey,
    val endPose: PoseKey,
    val setsRepsGuidance: String,
    val instructions: List<String>,
    val formCues: List<String>,
    val commonMistakes: List<String>
)

object ExerciseLibrary {

    val all: List<Exercise> = listOf(
        Exercise("squat", "Bodyweight Squat", ExerciseCategory.LEGS, Equipment.NONE, Difficulty.BEGINNER,
            listOf("Quadriceps", "Glutes", "Hamstrings"), PoseKey.STANDING, PoseKey.SQUAT_DOWN,
            "Beginner: 2-3 sets of 10-15 reps. Rest 60-90 seconds between sets.",
            listOf(
                "Stand with feet shoulder-width apart, toes slightly turned out.",
                "Brace your core and keep your chest up.",
                "Bend your knees and push your hips back as if sitting into a chair.",
                "Lower until your thighs are roughly parallel to the floor (or as low as you can control).",
                "Drive through your heels to stand back up."
            ),
            listOf("Keep knees tracking over your toes, not caving inward", "Keep weight in your heels and mid-foot", "Keep your chest lifted and spine neutral"),
            listOf("Knees collapsing inward", "Heels lifting off the floor", "Rounding the lower back at the bottom")),

        Exercise("pushup", "Push-Up", ExerciseCategory.CHEST, Equipment.NONE, Difficulty.BEGINNER,
            listOf("Chest", "Triceps", "Shoulders", "Core"), PoseKey.PLANK, PoseKey.PUSHUP_DOWN,
            "Beginner: 2-3 sets of 5-12 reps (knees-down version if needed). Rest 60-90 seconds.",
            listOf(
                "Start in a plank position with hands slightly wider than your shoulders.",
                "Keep your body in a straight line from head to heels.",
                "Bend your elbows to lower your chest toward the floor.",
                "Keep elbows at roughly 45° from your body rather than flared straight out.",
                "Press through your palms back to the start."
            ),
            listOf("Brace your core so hips neither sag nor pike", "Keep your neck neutral — look slightly ahead of your hands", "If full push-ups are too hard, use your knees but keep the straight torso line"),
            listOf("Hips sagging toward the floor", "Elbows flaring straight out to the sides", "Only lowering partway")),

        Exercise("plank", "Plank", ExerciseCategory.CORE, Equipment.NONE, Difficulty.BEGINNER,
            listOf("Core", "Shoulders", "Glutes"), PoseKey.PLANK, PoseKey.PLANK,
            "Beginner: 2-3 holds of 15-30 seconds. Build up gradually — quality over duration.",
            listOf(
                "Support yourself on your forearms and toes, elbows under your shoulders.",
                "Keep your body in one straight line from head to heels.",
                "Squeeze your glutes and brace your abdominals.",
                "Breathe steadily; stop the set when your hips start to sag or rise."
            ),
            listOf("Imagine drawing your belly button toward your spine", "Keep your head in line with your spine", "A shorter clean hold beats a longer sagging one"),
            listOf("Hips sagging (arching the lower back)", "Hips piked too high", "Holding your breath")),

        Exercise("lunge", "Forward Lunge", ExerciseCategory.LEGS, Equipment.NONE, Difficulty.BEGINNER,
            listOf("Quadriceps", "Glutes", "Hamstrings"), PoseKey.STANDING, PoseKey.LUNGE_FORWARD,
            "Beginner: 2-3 sets of 8-12 reps per leg.",
            listOf(
                "Stand tall with feet hip-width apart.",
                "Step forward and lower your hips until both knees are bent to about 90°.",
                "Keep your front knee roughly over your ankle.",
                "Push through your front heel to return to standing.",
                "Alternate legs, or finish all reps on one side first."
            ),
            listOf("Keep your torso upright", "Lower your back knee straight down", "Take a step long enough that the front knee doesn't travel far past the toes"),
            listOf("Front knee shooting far past the toes", "Leaning too far forward", "Short, wobbly steps")),

        Exercise("glute_bridge", "Glute Bridge", ExerciseCategory.LEGS, Equipment.NONE, Difficulty.BEGINNER,
            listOf("Glutes", "Hamstrings", "Lower back"), PoseKey.LYING_FLAT, PoseKey.BRIDGE_UP,
            "Beginner: 2-3 sets of 12-15 reps.",
            listOf(
                "Lie on your back with knees bent and feet flat, hip-width apart.",
                "Rest your arms at your sides.",
                "Squeeze your glutes and lift your hips until shoulders, hips and knees form a straight line.",
                "Pause briefly at the top, then lower with control."
            ),
            listOf("Drive through your heels", "Squeeze your glutes rather than over-arching your lower back", "Keep your ribs down at the top"),
            listOf("Over-arching the lower back", "Feet too far from the hips", "Rushing the movement")),

        Exercise("situp", "Sit-Up", ExerciseCategory.CORE, Equipment.NONE, Difficulty.BEGINNER,
            listOf("Abdominals", "Hip flexors"), PoseKey.LYING_FLAT, PoseKey.SITUP_UP,
            "Beginner: 2-3 sets of 10-15 reps.",
            listOf(
                "Lie on your back, knees bent, feet flat.",
                "Cross your arms over your chest or lightly touch your head with your fingertips.",
                "Curl your torso up toward your knees, exhaling as you rise.",
                "Lower back down with control."
            ),
            listOf("Lead with your chest rather than yanking your neck", "Keep your feet flat", "Move with control, not momentum"),
            listOf("Pulling on the neck", "Swinging up with momentum", "Feet lifting off the floor")),

        Exercise("jumping_jack", "Jumping Jack", ExerciseCategory.CARDIO, Equipment.NONE, Difficulty.BEGINNER,
            listOf("Full body", "Cardiovascular system"), PoseKey.STANDING, PoseKey.ARMS_UP_OUT,
            "Beginner: 2-3 rounds of 20-30 seconds.",
            listOf(
                "Start standing with feet together and arms at your sides.",
                "Jump your feet out wide while raising your arms overhead.",
                "Jump back to the start position.",
                "Repeat at a steady rhythm."
            ),
            listOf("Land softly with slightly bent knees", "Keep a steady rhythm", "Keep breathing — don't hold your breath"),
            listOf("Landing stiff-legged", "Arms lagging behind the legs")),

        Exercise("mountain_climber", "Mountain Climber", ExerciseCategory.CARDIO, Equipment.NONE, Difficulty.INTERMEDIATE,
            listOf("Core", "Hip flexors", "Shoulders", "Cardiovascular system"), PoseKey.PLANK, PoseKey.MOUNTAIN_CLIMBER,
            "Beginner: 2-3 rounds of 20-30 seconds.",
            listOf(
                "Start in a plank position with hands under your shoulders.",
                "Drive one knee toward your chest, then switch legs.",
                "Keep alternating at a pace you can control.",
                "Keep your hips low and core braced."
            ),
            listOf("Keep your hips level", "Land softly on the balls of your feet", "Start slowly, then build speed"),
            listOf("Hips rising too high", "Going so fast that form breaks down")),

        Exercise("bicep_curl", "Dumbbell Bicep Curl", ExerciseCategory.ARMS, Equipment.DUMBBELLS, Difficulty.BEGINNER,
            listOf("Biceps", "Forearms"), PoseKey.ARMS_DOWN, PoseKey.ARM_CURL_UP,
            "Beginner: 2-3 sets of 10-15 reps. Choose a weight where the last 2-3 reps are challenging but your form stays clean.",
            listOf(
                "Stand tall holding a dumbbell in each hand, palms facing forward.",
                "Keep your elbows close to your sides.",
                "Curl the weights toward your shoulders, squeezing your biceps.",
                "Lower back down with control to full extension."
            ),
            listOf("Keep your elbows pinned at your sides", "Don't heave the weight up with your back", "Control the lowering phase"),
            listOf("Swinging the torso", "Elbows drifting forward", "Using too heavy a weight")),

        Exercise("shoulder_press", "Dumbbell Shoulder Press", ExerciseCategory.SHOULDERS, Equipment.DUMBBELLS, Difficulty.INTERMEDIATE,
            listOf("Shoulders", "Triceps", "Upper chest"), PoseKey.ARM_CURL_UP, PoseKey.SHOULDER_PRESS_UP,
            "Beginner: 2-3 sets of 8-12 reps with a moderate, controllable weight.",
            listOf(
                "Stand or sit tall with a dumbbell in each hand at shoulder height, palms forward.",
                "Brace your core and keep your lower back neutral.",
                "Press the weights straight overhead until your arms are extended.",
                "Lower back to shoulder height with control."
            ),
            listOf("Keep your ribs down — don't over-arch to press", "Press in a controlled, straight line", "Start light until the pattern feels stable"),
            listOf("Excessive lower-back arch", "Flaring elbows too wide", "Using leg drive instead of a strict press")),

        Exercise("rdl", "Dumbbell Romanian Deadlift", ExerciseCategory.BACK, Equipment.DUMBBELLS, Difficulty.INTERMEDIATE,
            listOf("Hamstrings", "Glutes", "Lower back"), PoseKey.STANDING, PoseKey.HINGE_DEADLIFT,
            "Beginner: 2-3 sets of 8-12 reps. Start light to learn the hip-hinge before adding weight.",
            listOf(
                "Stand holding dumbbells in front of your thighs, feet hip-width apart.",
                "Keep a soft bend in your knees — this is a hip hinge, not a squat.",
                "Push your hips straight back, sliding the weights down your legs with a flat back.",
                "Lower until you feel a hamstring stretch, then drive your hips forward to stand."
            ),
            listOf("Keep the weights close to your legs", "Keep your spine neutral", "Think \"hips back\" rather than \"knees bend\""),
            listOf("Rounding the lower back", "Turning it into a squat", "Letting the weights drift away from the legs")),

        Exercise("burpee", "Burpee", ExerciseCategory.FULL_BODY, Equipment.NONE, Difficulty.ADVANCED,
            listOf("Full body", "Cardiovascular system"), PoseKey.STANDING, PoseKey.PUSHUP_DOWN,
            "Beginner: 2-3 sets of 5-10 reps, resting as needed — this is demanding, go at your own pace.",
            listOf(
                "Start standing, then squat down and place your hands on the floor.",
                "Step or jump your feet back into a plank.",
                "Optional: do a push-up.",
                "Step or jump your feet back to your hands.",
                "Stand (or jump) up, reaching your arms overhead."
            ),
            listOf("Step instead of jumping if the full version is too hard on your joints", "Keep your core braced in the plank", "Land softly with bent knees"),
            listOf("Lower back sagging in the plank", "Rushing until form collapses", "Landing stiff-legged"))
    )

    fun byCategory(cat: ExerciseCategory): List<Exercise> = all.filter { it.category == cat }
    fun find(id: String): Exercise? = all.find { it.id == id }
}
