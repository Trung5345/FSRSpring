-- ──────────────────────────────────────────────
-- 1. Topics (categories / chủ đề)
-- ──────────────────────────────────────────────
INSERT INTO topic (name, slug, description, icon_emoji, color_hex) VALUES
('Animals', 'animals', 'Vocabulary about animals and wildlife', '', '#4ade80'),
('Food & Drink', 'food', 'Food, drinks, and cooking vocabulary', '', '#fbbf24'),
('Technology', 'technology', 'Tech, software, and digital world terms', '', '#38bdf8'),
('Nature', 'nature', 'Nature, geography, and environment', '', '#86efac'),
('Emotions', 'emotions', 'Feelings and emotional states', '', '#fb923c'),
('Travel', 'travel', 'Travel, tourism, and transportation', '', '#a78bfa'),
('Business', 'business', 'Business, finance, and work vocabulary', '', '#f472b6'),
('Health', 'health', 'Health, medicine, and body vocabulary', '', '#34d399'),
('Education', 'education', 'Learning, school, and academic terms', '', '#60a5fa'),
('Arts & Culture', 'arts', 'Art, music, literature, and culture', '', '#f9a8d4');

-- ──────────────────────────────────────────────
-- 2. Words (with topic_id FK via subquery, cefr_level, part_of_speech)
-- ──────────────────────────────────────────────
INSERT INTO words (word, translation, example, pronunciation, category, difficulty, created_at, topic_id, cefr_level, part_of_speech, synonyms, antonyms) VALUES

-- Animals (A1-C1)
('cat',        'con mèo',      'The cat is sleeping on the sofa.',               '/kæt/',            'Animals', 'BEGINNER',     CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='animals'), 'A1', 'noun',      'feline, kitty',           'dog'),
('dog',        'con chó',      'My dog loves to play fetch.',                    '/dɒɡ/',            'Animals', 'BEGINNER',     CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='animals'), 'A1', 'noun',      'canine, hound, pup',      'cat'),
('elephant',   'con voi',      'The elephant is the largest land animal.',       '/ˈɛlɪfənt/',      'Animals', 'BEGINNER',     CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='animals'), 'A2', 'noun',      null,                      null),
('butterfly',  'con bướm',     'The butterfly landed on a flower.',              '/ˈbʌtəflaɪ/',     'Animals', 'INTERMEDIATE', CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='animals'), 'B1', 'noun',      null,                      null),
('cheetah',    'con báo đốm',  'The cheetah is the fastest land animal.',        '/ˈtʃiːtə/',       'Animals', 'INTERMEDIATE', CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='animals'), 'B1', 'noun',      null,                      null),
('predator',   'thú săn mồi', 'The lion is a fierce predator.',                  '/ˈprɛdətər/',     'Animals', 'ADVANCED',     CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='animals'), 'C1', 'noun',      'hunter, carnivore',       'prey'),

-- Food & Drink (A1-C1)
('apple',      'quả táo',      'An apple a day keeps the doctor away.',          '/ˈæpəl/',         'Food', 'BEGINNER',     CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='food'), 'A1', 'noun',      'fruit',                   null),
('bread',      'bánh mì',      'She baked fresh bread this morning.',            '/brɛd/',           'Food', 'BEGINNER',     CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='food'), 'A1', 'noun',      'loaf',                    null),
('noodle',     'mì sợi',       'Vietnamese pho is a famous noodle soup.',        '/ˈnuːdəl/',       'Food', 'BEGINNER',     CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='food'), 'A2', 'noun',      'pasta',                   null),
('avocado',    'quả bơ',       'Avocado is rich in healthy fats.',               '/ˌævəˈkɑːdoʊ/',  'Food', 'INTERMEDIATE', CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='food'), 'B1', 'noun',      null,                      null),
('persimmon',  'quả hồng',     'The persimmon tree bears fruit in autumn.',      '/pərˈsɪmən/',     'Food', 'ADVANCED',     CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='food'), 'C1', 'noun',      null,                      null),
('cuisine',    'ẩm thực',      'Italian cuisine is popular worldwide.',          '/kwɪˈziːn/',      'Food', 'INTERMEDIATE', CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='food'), 'B2', 'noun',      'cooking, gastronomy',     null),

-- Technology (A2-C2)
('computer',      'máy tính',          'She works on her computer every day.',             '/kəmˈpjuːtər/',   'Technology', 'BEGINNER',     CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='technology'), 'A2', 'noun',      'PC, laptop',              null),
('software',      'phần mềm',          'He is a software engineer at a tech company.',     '/ˈsɒftweər/',     'Technology', 'INTERMEDIATE', CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='technology'), 'B1', 'noun',      'program, application',    'hardware'),
('algorithm',     'thuật toán',        'The search algorithm returns results quickly.',    '/ˈælɡərɪðəm/',   'Technology', 'ADVANCED',     CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='technology'), 'C1', 'noun',      'procedure, method',       null),
('database',      'cơ sở dữ liệu',     'All user data is stored in a database.',          '/ˈdeɪtəbeɪs/',   'Technology', 'INTERMEDIATE', CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='technology'), 'B2', 'noun',      'datastore, repository',   null),
('cybersecurity', 'an ninh mạng',      'Cybersecurity is crucial in the digital age.',    '/ˌsaɪbəsɪˈkjʊərɪti/', 'Technology', 'ADVANCED', CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='technology'), 'C1', 'noun',   null,                      null),
('encryption',    'mã hóa',            'Encryption protects data from unauthorized access.','/ɪnˈkrɪpʃən/', 'Technology', 'ADVANCED',     CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='technology'), 'C1', 'noun',      'encoding, ciphering',     'decryption'),

-- Nature (A1-C1)
('mountain',    'núi',          'We hiked to the top of the mountain.',              '/ˈmaʊntɪn/',     'Nature', 'BEGINNER',     CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='nature'), 'A1', 'noun',      'peak, summit',            'valley'),
('river',       'sông',         'The river flows through the valley.',               '/ˈrɪvər/',       'Nature', 'BEGINNER',     CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='nature'), 'A1', 'noun',      'stream, brook',           null),
('forest',      'rừng',         'The Amazon is the world''s largest forest.',        '/ˈfɒrɪst/',      'Nature', 'BEGINNER',     CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='nature'), 'A2', 'noun',      'woodland, jungle',        'desert'),
('waterfall',   'thác nước',    'The waterfall was breathtakingly beautiful.',       '/ˈwɔːtərfɔːl/', 'Nature', 'INTERMEDIATE', CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='nature'), 'B1', 'noun',      'cascade, cataract',       null),
('archipelago', 'quần đảo',     'Vietnam has a beautiful archipelago in the East Sea.', '/ˌɑːrkɪˈpɛləɡoʊ/', 'Nature', 'ADVANCED', CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='nature'), 'C1', 'noun', 'island chain',            null),
('ecosystem',   'hệ sinh thái', 'Deforestation destroys the local ecosystem.',       '/ˈiːkoʊˌsɪstəm/', 'Nature', 'ADVANCED',   CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='nature'), 'C1', 'noun',      'biome, habitat',          null),

-- Emotions (A1-C1)
('happy',       'vui vẻ',       'She felt happy when she passed the exam.',          '/ˈhæpi/',        'Emotions', 'BEGINNER',     CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='emotions'), 'A1', 'adjective', 'joyful, content, glad',   'sad, unhappy'),
('sad',         'buồn',         'He was sad when his team lost the game.',            '/sæd/',          'Emotions', 'BEGINNER',     CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='emotions'), 'A1', 'adjective', 'unhappy, sorrowful',       'happy, joyful'),
('excited',     'hào hứng',     'The children were excited about the trip.',         '/ɪkˈsaɪtɪd/',   'Emotions', 'INTERMEDIATE', CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='emotions'), 'B1', 'adjective', 'thrilled, eager',          'bored, indifferent'),
('melancholy',  'u sầu',        'The rainy day brought a feeling of melancholy.',    '/ˈmɛlənkɒli/',  'Emotions', 'ADVANCED',     CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='emotions'), 'C1', 'noun',      'sadness, gloom, sorrow',   'joy, happiness'),
('nostalgic',   'hoài niệm',    'Looking at old photos made her feel nostalgic.',    '/nɒˈstældʒɪk/', 'Emotions', 'ADVANCED',     CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='emotions'), 'C1', 'adjective', 'sentimental, wistful',     null),
('euphoric',    'phấn khích',   'She felt euphoric after winning the award.',        '/juːˈfɒrɪk/',   'Emotions', 'ADVANCED',     CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='emotions'), 'C1', 'adjective', 'elated, ecstatic',         'miserable, despondent'),

-- Travel (A1-C1)
('passport',      'hộ chiếu',       'Don''t forget your passport when traveling abroad.',  '/ˈpɑːspɔːrt/',  'Travel', 'BEGINNER',     CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='travel'), 'A2', 'noun',      'travel document',         null),
('hotel',         'khách sạn',      'We booked a hotel near the beach.',                  '/hoʊˈtɛl/',    'Travel', 'BEGINNER',     CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='travel'), 'A1', 'noun',      'accommodation, inn',      null),
('itinerary',     'lịch trình',     'She planned every detail in her itinerary.',         '/aɪˈtɪnəreri/','Travel', 'ADVANCED',     CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='travel'), 'C1', 'noun',      'schedule, plan',          null),
('souvenir',      'đồ lưu niệm',    'He bought a souvenir from every city he visited.',   '/ˌsuːvəˈnɪər/','Travel', 'INTERMEDIATE', CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='travel'), 'B1', 'noun',      'memento, keepsake',       null),
('accommodation', 'chỗ ở',          'They found affordable accommodation near the center.','/əˌkɒməˈdeɪʃən/','Travel', 'ADVANCED', CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='travel'), 'B2', 'noun',     'lodging, housing',        null),
('layover',       'quá cảnh',       'We had a six-hour layover at Dubai Airport.',        '/ˈleɪoʊvər/',  'Travel', 'INTERMEDIATE', CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='travel'), 'B2', 'noun',      'stopover, transit',       null),

-- Business (B1-C2)
('negotiate',    'đàm phán',     'They spent hours trying to negotiate a deal.',          '/nɪˈɡoʊʃieɪt/', 'Business', 'INTERMEDIATE', CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='business'), 'B2', 'verb',     'bargain, discuss',        null),
('revenue',      'doanh thu',    'The company''s revenue grew by 20% this year.',         '/ˈrɛvɪnjuː/',  'Business', 'INTERMEDIATE', CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='business'), 'B2', 'noun',     'income, earnings',        'expenditure'),
('entrepreneur', 'doanh nhân',   'She is a successful entrepreneur with three startups.', '/ˌɒntrəprəˈnɜːr/','Business','ADVANCED', CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='business'), 'C1', 'noun',  'founder, innovator',      null),
('stakeholder',  'bên liên quan','All stakeholders were invited to the meeting.',         '/ˈsteɪkhoʊldər/','Business','ADVANCED',  CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='business'), 'C1', 'noun',  'shareholder, participant',null),

-- Health (A2-C1)
('medicine',    'thuốc',        'Take this medicine twice a day.',                       '/ˈmɛdɪsɪn/',   'Health', 'BEGINNER',     CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='health'), 'A2', 'noun',      'drug, medication',        null),
('symptom',     'triệu chứng',  'Fatigue is a common symptom of the flu.',               '/ˈsɪmptəm/',   'Health', 'INTERMEDIATE', CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='health'), 'B2', 'noun',      'sign, indication',        null),
('immunity',    'miễn dịch',    'Regular exercise can boost your immunity.',             '/ɪˈmjuːnɪti/', 'Health', 'ADVANCED',     CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='health'), 'C1', 'noun',      'resistance, defense',     'susceptibility'),

-- Education (A2-C1)
('curriculum',  'chương trình học', 'The school updated its science curriculum.',        '/kəˈrɪkjʊləm/', 'Education', 'ADVANCED',   CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='education'), 'C1', 'noun',   'syllabus, program',       null),
('scholarship', 'học bổng',         'She received a full scholarship to university.',   '/ˈskɒlərʃɪp/', 'Education', 'INTERMEDIATE', CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='education'), 'B2', 'noun',  'grant, fellowship, award',null),
('thesis',      'luận văn',         'He spent two years writing his doctoral thesis.',  '/ˈθiːsɪs/',    'Education', 'ADVANCED',     CURRENT_TIMESTAMP, (SELECT id FROM topic WHERE slug='education'), 'C1', 'noun',   'dissertation, paper',     null);

