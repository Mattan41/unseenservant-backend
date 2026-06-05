-- =====================================================================================
-- USERS
-- Password for all users is 'password'
-- =====================================================================================
INSERT INTO users (id, user_name, full_name, email, provider_id, provider_type, role, password, created_at, updated_at)
VALUES (1, 'User1', 'User One', 'user1@example.com', '111111', 'GOOGLE', 'USER',
        '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (2, 'User2', 'User Two', 'user2@example.com', '222222', 'GOOGLE', 'USER',
        '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (3, 'User3', 'User Three', 'user3@example.com', '333333', 'GOOGLE', 'USER',
        '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (4, 'User4', 'User Four', 'user4@example.com', '123456', 'GOOGLE', 'USER',
        '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (5, 'User5', 'User Five', 'user5@example.com', '654321', 'GOOGLE', 'USER',
        '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (6, 'admin', 'Admin', 'admin@admin.se', '000000', 'GITHUB', 'ADMIN',
        '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);


-- =====================================================================================
-- CAMPAIGNS
-- =====================================================================================
INSERT INTO campaign (id, name, description, owner_id, image_url, created_by, last_modified_by, created_at, updated_at)
VALUES (1, 'Curse of Strahd',
        'In the age of steam and gears, the Iron Colossus cuts through rugged mountain passes and
                        sprawling industrial cities?an unstoppable marvel of clockwork engineering. Tonight, its
                        armored compartments carry not just passengers and freight, but a mysterious cargo rumored
                        to change the fate of the Empire. As the city?s skyline vanishes in a cloud of smoke,
                        your crew boards the train with one goal: seize the prize before rivals, lawmen,
                        or deadly automata claim it for themselves.

                        Whistles scream, pistons thunder, and the race is on across perilous trestle bridges and volatile territories.
                        Every car holds secrets? cunning adversaries, exotic machinery, and deadly traps.
                        Will you outsmart the authorities, outfight mercenaries, and outpace the opposition,
                        or will the Iron Colossus become your tomb beneath the relentless steam and steel?',
        1, 'https://www.dndbeyond.com/attachments/8/220/cos-cover-4k.jpg', 'User4', 'User4', CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP),
       (2, 'Neptunus', 'The University of Lundenwic has been robbed of its prized artifact, the Amulet of Taharka. The players are hired to track down the thief and retrieve the amulet.

                 Mystery & Investigation: Dive deep into a campaign filled with deception, riddles, and puzzles. Finding the thief will require sharp minds and keen eyes for detail.
                 Urban Adventure: Explore a vibrant city teeming with intrigue, hidden societies, eccentric professors, and shifting alliances.
                 Roleplay & Social Encounters: Forge alliances, interrogate suspects, and navigate the political landscape of scholars, nobles, and underground factions.
                 Skill-Based Challenges: Success will depend not just on combat prowess, but also on investigation, stealth, persuasion, and strategy.
                 Dynamic Consequences: Choices matter?a trusting word or a misstep could change the course of your investigation, and the fate of Lundenwic itself.',
        2,
        'https://c4.wallpaperflare.com/wallpaper/925/634/481/league-of-legends-bilgewater-fantasy-art-pirates-wallpaper-preview.jpg',
        'User5', 'User5', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (3, 'Gears of War', 'In the age of steam and gears, the Iron Colossus cuts through rugged mountain passes and
                        sprawling industrial cities?an unstoppable marvel of clockwork engineering. Tonight, its
                        armored compartments carry not just passengers and freight, but a mysterious cargo rumored
                        to change the fate of the Empire. As the city?s skyline vanishes in a cloud of smoke,
                        your crew boards the train with one goal: seize the prize before rivals, lawmen,
                        or deadly automata claim it for themselves.

                        Whistles scream, pistons thunder, and the race is on across perilous trestle bridges and volatile territories.
                        Every car holds secrets? cunning adversaries, exotic machinery, and deadly traps.
                        Will you outsmart the authorities, outfight mercenaries, and outpace the opposition,
                        or will the Iron Colossus become your tomb beneath the relentless steam and steel?',
        2,
        'https://slyflourish.com/images/eberron_warforged.jpg', 'User5', 'User5', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);


-- =====================================================================================
-- CAMPAIGN USERS (Participants)
-- =====================================================================================
INSERT INTO campaign_user (campaign_id, user_id, role, nickname)
VALUES
-- Campaign 1
(1, 1, 'GM', 'User1'),
(1, 2, 'PLAYER', 'user2'),
(1, 3, 'PLAYER', 'User3'),
(1, 4, 'PLAYER', 'User4'),
-- Campaign 2
(2, 1, 'PLAYER', 'User1'),
(2, 2, 'GM', 'User2'),
(2, 3, 'PLAYER', 'User3'),
(2, 4, 'PLAYER', 'User4'),
-- Campaign 3
(3, 1, 'PLAYER', 'User1'),
(3, 2, 'PLAYER', 'User2'),
(3, 3, 'GM', 'User3'),
(3, 4, 'PLAYER', 'User4');


-- =====================================================================================
-- MESSAGES
-- =====================================================================================
INSERT INTO messages (id, campaign_id, user_id, message_body, created_at, updated_at)
VALUES (1, 1, 1, 'Wow this campaign is great!', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (2, 1, 2, 'I agree! What character are you going to play?', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (3, 1, 3, 'I am going to play a wizard!', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (4, 2, 2, 'Hello, I am new to this campaign!', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (5, 2, 4, 'Welcome! Any questions?', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);


-- =====================================================================================
-- PLAYER CHARACTERS
-- =====================================================================================
INSERT INTO game_character (id, owner_id, campaign_id, name, level, character_class, race, character_data, created_at,
                            updated_at)
VALUES
-- Campaign 1
(1, 1, 1, 'Gandalf', 10, 'Wizard', 'Human',
 '{"strength":11, "dexterity":11, "constitution":11, "intelligence":11, "wisdom":15, "charisma":13}', CURRENT_TIMESTAMP,
 CURRENT_TIMESTAMP),
(2, 2, 1, 'Frodo', 8, 'Rogue', 'Halfling',
 '{"strength":10, "dexterity":18, "constitution":12, "intelligence":12, "wisdom":13, "charisma":10}', CURRENT_TIMESTAMP,
 CURRENT_TIMESTAMP),
(3, 3, 1, 'Galadriel', 10, 'Cleric', 'Elf',
 '{"strength":10, "dexterity":10, "constitution":10, "intelligence":14, "wisdom":18, "charisma":16}', CURRENT_TIMESTAMP,
 CURRENT_TIMESTAMP),
(4, 4, 1, 'Gimli', 8, 'Fighter', 'Dwarf',
 '{"strength":19, "dexterity":8, "constitution":16, "intelligence":9, "wisdom":11, "charisma":8}', CURRENT_TIMESTAMP,
 CURRENT_TIMESTAMP),

-- Campaign 2
(5, 1, 2, 'Rincewind', 1, 'Wizard', 'Human',
 '{"strength":9, "dexterity":9, "constitution":9, "intelligence":14, "wisdom":9, "charisma":9}', CURRENT_TIMESTAMP,
 CURRENT_TIMESTAMP),
(6, 2, 2, 'Twoflower', 1, 'Rogue', 'Halfling',
 '{"strength":9, "dexterity":14, "constitution":9, "intelligence":9, "wisdom":9, "charisma":14}', CURRENT_TIMESTAMP,
 CURRENT_TIMESTAMP),
(7, 3, 2, 'Mightily Oats', 1, 'Cleric', 'Human',
 '{"strength":10, "dexterity":10, "constitution":10, "intelligence":10, "wisdom":16, "charisma":10}', CURRENT_TIMESTAMP,
 CURRENT_TIMESTAMP),
(8, 4, 2, 'Carrot Ironfoundersson', 1, 'Fighter', 'Dwarf',
 '{"strength":16, "dexterity":10, "constitution":14, "intelligence":10, "wisdom":14, "charisma":12}', CURRENT_TIMESTAMP,
 CURRENT_TIMESTAMP),

-- Campaign 3
(9, 1, 3, 'Ironclad', 1, 'Paladin', 'Dragonborn',
 '{"strength":10, "dexterity":10, "constitution":10, "intelligence":10, "wisdom":10, "charisma":10}', CURRENT_TIMESTAMP,
 CURRENT_TIMESTAMP),
(10, 2, 3, 'Gearspark', 1, 'Rogue', 'Gnome',
 '{"strength":10, "dexterity":10, "constitution":10, "intelligence":10, "wisdom":10, "charisma":10}', CURRENT_TIMESTAMP,
 CURRENT_TIMESTAMP),
(11, 3, 3, 'Steamwhistle', 1, 'Bard', 'Halfling',
 '{"strength":10, "dexterity":10, "constitution":10, "intelligence":10, "wisdom":10, "charisma":10}', CURRENT_TIMESTAMP,
 CURRENT_TIMESTAMP),
(12, 4, 3, 'Cogsworth', 1, 'Fighter', 'Human',
 '{"strength":10, "dexterity":10, "constitution":10, "intelligence":10, "wisdom":10, "charisma":10}', CURRENT_TIMESTAMP,
 CURRENT_TIMESTAMP);