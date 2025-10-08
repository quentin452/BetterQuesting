# BetterQuesting DSL Test File - All Task and Reward Types
# This file tests all available task and reward types to ensure they work correctly
@mod TestMod
@filename test_all_types.dsl
@category testing
@quest_line Testing
@layout GRID
@spacing_x 40
@spacing_y 40
@base_x 0
@base_y 0

# ========================================
# TASK TYPE TESTS
# ========================================

>quest test_checkbox
    title: Test: Checkbox Task
    desc: Manual completion task - just click to complete
    requires: none
    logic: AND
    task: checkbox "Click to complete this quest"
    reward: xp 10
    repeatable: no
    auto_claim: no
    icon: minecraft:book

>quest test_craft
    title: Test: Craft Task
    desc: Craft a wooden pickaxe
    requires: test_checkbox
    logic: AND
    task: craft "minecraft:wooden_pickaxe" 1
    reward: xp 20
    repeatable: no
    auto_claim: no
    icon: minecraft:wooden_pickaxe

>quest test_collect
    title: Test: Collect Task
    desc: Collect and submit 10 cobblestone
    requires: test_checkbox
    logic: AND
    task: collect "minecraft:cobblestone" 10
    reward: xp 20
    repeatable: no
    auto_claim: no
    icon: minecraft:cobblestone 10

>quest test_kill
    title: Test: Kill Task
    desc: Kill 5 zombies
    requires: test_checkbox
    logic: AND
    task: kill "Zombie" 5
    reward: xp 30
    repeatable: no
    auto_claim: no

>quest test_visit_dimension
    title: Test: Visit Dimension
    desc: Visit the Nether (dimension -1)
    requires: test_checkbox
    logic: AND
    task: visit -1
    reward: xp 50
    repeatable: no
    auto_claim: no

>quest test_visit_coords
    title: Test: Visit Coordinates
    desc: Visit specific coordinates in the Overworld
    requires: test_checkbox
    logic: AND
    task: visit 0 100 64 100
    reward: xp 40
    repeatable: no
    auto_claim: no

>quest test_visit_biome
    title: Test: Visit Biome
    desc: Visit a Desert biome
    requires: test_checkbox
    logic: AND
    task: visit 0 biome 2
    reward: xp 40
    repeatable: no
    auto_claim: no

>quest test_interact
    title: Test: Interact Task
    desc: Use a flint and steel to start a fire
    requires: test_checkbox
    logic: AND
    task: interact "minecraft:flint_and_steel" 1 "Start a fire"
    reward: xp 25
    repeatable: no
    auto_claim: no

>quest test_blockbreak
    title: Test: Block Break Task
    desc: Break 20 dirt blocks
    requires: test_checkbox
    logic: AND
    task: blockbreak "minecraft:dirt" 20
    reward: xp 15
    repeatable: no
    auto_claim: no

>quest test_fluid
    title: Test: Fluid Task
    desc: Collect 1000mb (1 bucket) of water
    requires: test_checkbox
    logic: AND
    task: fluid "water" 1000
    reward: xp 30
    repeatable: no
    auto_claim: no

>quest test_meeting
    title: Test: Meeting Task
    desc: Encounter a Creeper (don't need to kill it)
    requires: test_checkbox
    logic: AND
    task: meeting "Creeper"
    reward: xp 20
    repeatable: no
    auto_claim: no

>quest test_xp_task
    title: Test: XP Task
    desc: Gain 100 experience points
    requires: test_checkbox
    logic: AND
    task: xp 100
    reward: xp 50
    repeatable: no
    auto_claim: no

>quest test_scoreboard
    title: Test: Scoreboard Task
    desc: Reach a score of 10 in objective 'test_score' (use /scoreboard command to set it)
    requires: test_checkbox
    logic: AND
    task: scoreboard test_score 10
    reward: xp 40
    repeatable: no
    auto_claim: no

# ========================================
# REWARD TYPE TESTS
# ========================================

>quest test_reward_xp
    title: Test: XP Reward
    desc: Complete to receive 100 XP
    requires: test_checkbox
    logic: AND
    task: checkbox "Complete for XP"
    reward: xp 100
    repeatable: no
    auto_claim: no

>quest test_reward_item
    title: Test: Item Reward
    desc: Complete to receive 16 torches
    requires: test_checkbox
    logic: AND
    task: checkbox "Complete for torches"
    reward: item minecraft:torch 16 "Torches"
    repeatable: no
    auto_claim: no

>quest test_reward_choice
    title: Test: Choice Reward
    desc: Complete to choose one of multiple rewards
    requires: test_checkbox
    logic: AND
    task: checkbox "Complete for choice"
    reward: choice
        minecraft:iron_ingot 5
        minecraft:gold_ingot 3
        minecraft:diamond 1
    repeatable: no
    auto_claim: no

>quest test_reward_command
    title: Test: Command Reward
    desc: Complete to execute a server command (time set day)
    requires: test_checkbox
    logic: AND
    task: checkbox "Complete for command"
    reward: command "time set day"
    repeatable: no
    auto_claim: no

>quest test_reward_scoreboard
    title: Test: Scoreboard Reward (Part 1)
    desc: Complete to add 5 to scoreboard objective 'points_chain'
    requires: test_checkbox
    logic: AND
    task: checkbox "Complete for scoreboard points"
    reward: scoreboard points_chain 5
    repeatable: no
    auto_claim: no

>quest test_scoreboard_chain
    title: Test: Scoreboard Chain (Part 2)
    desc: Reach 5 points in 'points_chain' (earned from previous quest)
    requires: test_reward_scoreboard
    logic: AND
    task: scoreboard points_chain 5
    reward: xp 50
    repeatable: no
    auto_claim: no

>quest test_reward_quest
    title: Test: Quest Completion Reward
    desc: Completing this quest also completes another quest
    requires: test_checkbox
    logic: AND
    task: checkbox "Complete for quest unlock"
    reward: questcompletion "test_checkbox"
    repeatable: no
    auto_claim: no

# ========================================
# LOGIC TYPE TESTS
# ========================================

>quest test_logic_and
    title: Test: AND Logic
    desc: Requires both test_craft AND test_collect to be completed
    requires: test_craft, test_collect
    logic: AND
    task: checkbox "Both prerequisites must be complete"
    reward: xp 30
    repeatable: no
    auto_claim: no

>quest test_logic_or
    title: Test: OR Logic
    desc: Requires either test_craft OR test_collect to be completed
    requires: test_craft, test_collect
    logic: OR
    task: checkbox "Either prerequisite must be complete"
    reward: xp 30
    repeatable: no
    auto_claim: no

>quest test_logic_xor
    title: Test: XOR Logic
    desc: Requires exactly one of test_craft or test_collect (not both)
    requires: test_craft, test_collect
    logic: XOR
    task: checkbox "Exactly one prerequisite must be complete"
    reward: xp 30
    repeatable: no
    auto_claim: no

>quest test_logic_nand
    title: Test: NAND Logic
    desc: Requires NOT all of test_craft and test_collect (at least one incomplete)
    requires: test_craft, test_collect
    logic: NAND
    task: checkbox "Not both prerequisites complete"
    reward: xp 30
    repeatable: no
    auto_claim: no

>quest test_logic_nor
    title: Test: NOR Logic
    desc: Requires neither test_craft nor test_collect to be completed
    requires: test_craft, test_collect
    logic: NOR
    task: checkbox "Neither prerequisite should be complete"
    reward: xp 30
    repeatable: no
    auto_claim: no

>quest test_logic_xnor
    title: Test: XNOR Logic
    desc: Requires either both complete OR neither complete
    requires: test_craft, test_collect
    logic: XNOR
    task: checkbox "Either both or neither prerequisite complete"
    reward: xp 30
    repeatable: no
    auto_claim: no

# ========================================
# QUEST PROPERTY TESTS
# ========================================

>quest test_repeatable
    title: Test: Repeatable Quest
    desc: This quest can be repeated after completion
    requires: test_checkbox
    logic: AND
    task: checkbox "Complete to test repeatability"
    reward: xp 10
    repeatable: yes
    auto_claim: no

>quest test_auto_claim
    title: Test: Auto-Claim Quest
    desc: Rewards are automatically claimed when quest completes
    requires: test_checkbox
    logic: AND
    task: checkbox "Complete to test auto-claim"
    reward: xp 25
    repeatable: no
    auto_claim: yes

>quest test_multiple_prereqs
    title: Test: Multiple Prerequisites
    desc: Requires three different quests
    requires: test_craft, test_collect, test_kill
    logic: AND
    task: checkbox "Complete when all three prerequisites are done"
    reward: xp 100
    repeatable: no
    auto_claim: no

>quest test_no_prereqs
    title: Test: No Prerequisites
    desc: This quest has no prerequisites
    requires: none
    logic: AND
    task: checkbox "Available from the start"
    reward: xp 15
    repeatable: no
    auto_claim: no

# ========================================
# CROSS-QUESTLINE PREREQUISITE TEST
# ========================================

>quest test_cross_questline
    title: Test: Cross-Questline Reference
    desc: References a quest from a different questline (if exists)
    requires: Testing:test_checkbox
    logic: AND
    task: checkbox "Complete to test cross-questline prerequisites"
    reward: xp 40
    repeatable: no
    auto_claim: no

# ========================================
# TEXT COLOR AND FORMATTING TESTS
# ========================================

>quest test_text_color
    title: §6Golden §l§4DANGER!§r §bQuest
    desc: §7This quest tests §atext §bcolors§7 and §l§oformatting§r§7. Contains §e§lbold yellow§r§7, §c§nunderlined red§r§7, and §k§dmagic purple§r§7 text!
    requires: test_checkbox
    logic: AND
    task: checkbox "Complete to test text color support"
    reward: xp 50
    repeatable: no
    auto_claim: no
    icon: minecraft:dye 1 11

>quest test_bg_image
    title: §2Test: Background Image
    desc: This quest should display a custom background image (stone texture)
    requires: test_checkbox
    logic: AND
    task: checkbox "Complete to test background image"
    reward: xp 30
    repeatable: no
    auto_claim: no
    icon: minecraft:stone
    bg_image: minecraft:textures/blocks/stone.png
    bg_size: 256

>quest test_bg_image_large
    title: §4Test: Large Background
    desc: This quest tests a larger background image size (512 pixels)
    requires: test_bg_image
    logic: AND
    task: checkbox "Complete to test large background"
    reward: xp 35
    repeatable: no
    auto_claim: no
    icon: minecraft:obsidian
    bg_image: minecraft:textures/blocks/obsidian.png
    bg_size: 512
