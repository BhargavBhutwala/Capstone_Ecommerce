-- =============================================================================
-- E-Bookstore polished demo catalogue
-- =============================================================================
--
-- Purpose:
--   Local/demo data for the full-stack E-Bookstore capstone.
--
-- Prerequisite:
--   Flyway migrations V1-V12 must already be applied.
--   V12 adds products.image_url.
--
-- This is NOT a Flyway migration. Keep it under scripts/.
--
-- Safe to rerun:
--   * categories upsert by unique name
--   * brands upsert by unique name
--   * products upsert by unique ISBN
--
-- It does NOT delete users, carts, orders, payments, or existing unrelated
-- catalogue rows.
--
-- Book covers:
--   image_url is derived from ISBN using Open Library Covers.
--   "?default=false" lets broken/missing covers return an HTTP error so the
--   frontend can show its "No cover available" fallback.
--
-- Demo prices are sample catalogue values for this capstone, not live retail
-- prices.
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- Categories
-- -----------------------------------------------------------------------------

INSERT INTO categories (
    name,
    description,
    active,
    created_at,
    updated_at
)
VALUES
    ('Programming', 'Software engineering, programming languages, architecture, and developer practices.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Fiction', 'Classic and contemporary literary and general fiction.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Fantasy', 'Fantasy adventures, epic worlds, magic, and speculative storytelling.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Mystery & Thriller', 'Mystery, crime, suspense, and psychological thrillers.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Science', 'Accessible science covering physics, astronomy, biology, medicine, and the natural world.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('History', 'Books exploring civilizations, societies, people, and major historical developments.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Self-Help', 'Practical books focused on habits, focus, relationships, mindset, and personal effectiveness.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Business', 'Entrepreneurship, leadership, management, strategy, innovation, and company building.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO UPDATE
SET
    description = EXCLUDED.description,
    active = TRUE,
    updated_at = CURRENT_TIMESTAMP;

-- -----------------------------------------------------------------------------
-- Brands / publishers
-- -----------------------------------------------------------------------------

INSERT INTO brands (
    name,
    description,
    active,
    created_at,
    updated_at
)
VALUES
    ('Addison-Wesley', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Anchor Books', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Avery', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Ballantine Books', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Bantam Books', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Berkley', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Broadway Books', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Celadon Books', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Crown Business', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Crown Publishing', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('DAW Books', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Doubleday', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Gallery Books', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Grand Central Publishing', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Harper Perennial', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('HarperBusiness', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('HarperCollins', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('HarperOne', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Harvard Business Review Press', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Knopf Books for Young Readers', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Liveright', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Manning Publications', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Mariner Books', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('No Starch Press', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('O''Reilly Media', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Oxford University Press', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Penguin Classics', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Pocket Books', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Portfolio', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Prentice Hall', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Random House', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Riverhead Books', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Scholastic', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Scribner', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Signet Classics', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Simon & Schuster', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Tor Books', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Vintage Books', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('W. W. Norton', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('William Morrow', 'Demo catalogue publisher / imprint.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO UPDATE
SET
    description = EXCLUDED.description,
    active = TRUE,
    updated_at = CURRENT_TIMESTAMP;

-- -----------------------------------------------------------------------------
-- Products
-- -----------------------------------------------------------------------------

WITH seed_products (
    title,
    isbn,
    description,
    price,
    stock_quantity,
    category_name,
    brand_name,
    delivery_days_min,
    delivery_days_max,
    active
) AS (
    VALUES
        ('Clean Code', '9780132350884', 'Robert C. Martin presents practical principles for writing readable, maintainable, and professional software.', 799.00, 24, 'Programming', 'Prentice Hall', 2, 5, TRUE),
        ('Effective Java', '9780134685991', 'Joshua Bloch''s guide to effective Java design, APIs, generics, concurrency, and robust programming practices.', 899.00, 18, 'Programming', 'Addison-Wesley', 2, 5, TRUE),
        ('Head First Design Patterns', '9781492078005', 'A visual, example-driven introduction to reusable object-oriented design patterns and the principles behind them.', 999.00, 16, 'Programming', 'O''Reilly Media', 3, 6, TRUE),
        ('Java Concurrency in Practice', '9780321349606', 'A detailed guide to building correct, reliable, and maintainable concurrent applications on the Java platform.', 849.00, 12, 'Programming', 'Addison-Wesley', 3, 6, TRUE),
        ('Spring in Action', '9781617297571', 'A practical introduction to building modern Java applications with Spring and Spring Boot.', 949.00, 20, 'Programming', 'Manning Publications', 2, 5, TRUE),
        ('Python Crash Course', '9781718502703', 'A hands-on introduction to Python fundamentals followed by practical projects for building real applications.', 699.00, 22, 'Programming', 'No Starch Press', 2, 4, TRUE),
        ('Designing Data-Intensive Applications', '9781449373320', 'Martin Kleppmann explores reliable, scalable, and maintainable data systems and the tradeoffs behind modern architectures.', 1099.00, 14, 'Programming', 'O''Reilly Media', 3, 6, TRUE),
        ('The Pragmatic Programmer', '9780135957059', 'Practical techniques and habits for improving software craftsmanship, design, communication, and career longevity.', 849.00, 19, 'Programming', 'Addison-Wesley', 2, 5, TRUE),
        ('Refactoring', '9780134757599', 'Martin Fowler explains disciplined techniques for improving the internal design of existing code without changing its behavior.', 1049.00, 11, 'Programming', 'Addison-Wesley', 3, 6, TRUE),
        ('Learning React', '9781492051725', 'A modern introduction to React fundamentals, component design, hooks, state, and maintainable user interfaces.', 749.00, 17, 'Programming', 'O''Reilly Media', 2, 5, TRUE),
        ('Clean Architecture', '9780134494166', 'Robert C. Martin explains architectural boundaries, dependency rules, and principles for creating maintainable software systems.', 899.00, 15, 'Programming', 'Prentice Hall', 2, 5, TRUE),
        ('Domain-Driven Design', '9780321125217', 'Eric Evans introduces domain modeling patterns for tackling complex software systems and aligning code with business concepts.', 1099.00, 10, 'Programming', 'Addison-Wesley', 3, 7, TRUE),
        ('1984', '9780451524935', 'George Orwell''s dystopian novel about surveillance, authoritarian power, truth, and individual freedom.', 299.00, 32, 'Fiction', 'Signet Classics', 1, 4, TRUE),
        ('Animal Farm', '9780451526342', 'George Orwell''s political allegory about a farm revolution and the corruption of its original ideals.', 199.00, 28, 'Fiction', 'Signet Classics', 1, 4, TRUE),
        ('The Great Gatsby', '9780743273565', 'F. Scott Fitzgerald''s portrait of ambition, wealth, longing, and disillusionment in the Jazz Age.', 249.00, 25, 'Fiction', 'Scribner', 2, 4, TRUE),
        ('To Kill a Mockingbird', '9780061120084', 'Harper Lee''s novel about childhood, conscience, prejudice, and justice in the American South.', 299.00, 21, 'Fiction', 'HarperCollins', 2, 5, TRUE),
        ('The Alchemist', '9780062315007', 'Paulo Coelho''s philosophical novel about a young traveler pursuing a dream and discovering purpose along the way.', 349.00, 26, 'Fiction', 'HarperCollins', 2, 5, TRUE),
        ('The Martian', '9780553418026', 'Andy Weir''s survival story about an astronaut using science, engineering, and persistence after being stranded on Mars.', 399.00, 18, 'Fiction', 'Crown Publishing', 2, 5, TRUE),
        ('The Book Thief', '9780375842207', 'Markus Zusak tells a story of books, friendship, loss, and courage in wartime Germany.', 349.00, 20, 'Fiction', 'Knopf Books for Young Readers', 2, 5, TRUE),
        ('The Kite Runner', '9781594631931', 'Khaled Hosseini''s novel of friendship, guilt, family, and redemption across decades of change in Afghanistan.', 349.00, 17, 'Fiction', 'Riverhead Books', 2, 5, TRUE),
        ('Little Women', '9780147514011', 'Louisa May Alcott''s enduring story of the March sisters, family, ambition, and growing up.', 249.00, 22, 'Fiction', 'Penguin Classics', 2, 4, TRUE),
        ('Pride and Prejudice', '9780141439518', 'Jane Austen''s classic novel of wit, family expectations, first impressions, and the evolving relationship between Elizabeth Bennet and Mr. Darcy.', 249.00, 24, 'Fiction', 'Penguin Classics', 2, 4, TRUE),
        ('The Hobbit', '9780547928227', 'J. R. R. Tolkien''s adventure following Bilbo Baggins from the Shire into a journey of dragons, riddles, and unexpected courage.', 349.00, 23, 'Fantasy', 'Mariner Books', 2, 5, TRUE),
        ('The Fellowship of the Ring', '9780547928210', 'The first volume of Tolkien''s epic journey across Middle-earth as a fellowship sets out to confront a growing darkness.', 399.00, 19, 'Fantasy', 'Mariner Books', 2, 5, TRUE),
        ('Harry Potter and the Sorcerer''s Stone', '9780590353427', 'J. K. Rowling introduces Harry Potter, Hogwarts, and a hidden magical world filled with friendship, danger, and discovery.', 299.00, 30, 'Fantasy', 'Scholastic', 1, 4, TRUE),
        ('A Game of Thrones', '9780553593716', 'George R. R. Martin begins an epic struggle for power among noble houses in a dangerous and politically complex fantasy world.', 449.00, 18, 'Fantasy', 'Bantam Books', 2, 5, TRUE),
        ('The Name of the Wind', '9780756404741', 'Patrick Rothfuss tells the story of Kvothe, a gifted musician and magician recounting the truth behind his legend.', 399.00, 15, 'Fantasy', 'DAW Books', 3, 6, TRUE),
        ('Mistborn: The Final Empire', '9780765350381', 'Brandon Sanderson launches a fantasy saga about rebellion, metal-based magic, and an empire that has endured for a thousand years.', 399.00, 21, 'Fantasy', 'Tor Books', 2, 5, TRUE),
        ('The Way of Kings', '9780765365279', 'Brandon Sanderson opens an expansive epic fantasy of war, honor, ancient powers, and intersecting destinies.', 499.00, 13, 'Fantasy', 'Tor Books', 3, 6, TRUE),
        ('The Lion, the Witch and the Wardrobe', '9780064471046', 'C. S. Lewis''s classic fantasy adventure in which four siblings enter Narnia and become part of a struggle against an endless winter.', 199.00, 25, 'Fantasy', 'HarperCollins', 2, 4, TRUE),
        ('Gone Girl', '9780307588371', 'Gillian Flynn''s psychological thriller about a marriage, a disappearance, and the stories people construct about one another.', 399.00, 18, 'Mystery & Thriller', 'Crown Publishing', 2, 5, TRUE),
        ('The Girl with the Dragon Tattoo', '9780307454546', 'Stieg Larsson''s mystery follows an investigative journalist and a gifted hacker uncovering a decades-old family secret.', 349.00, 14, 'Mystery & Thriller', 'Vintage Books', 2, 5, TRUE),
        ('The Silent Patient', '9781250301697', 'Alex Michaelides''s psychological thriller centers on a woman who stops speaking after a shocking act and the therapist determined to understand why.', 399.00, 22, 'Mystery & Thriller', 'Celadon Books', 2, 5, TRUE),
        ('The Da Vinci Code', '9780307474278', 'Dan Brown''s fast-paced thriller follows clues hidden in art, history, and secret societies across Europe.', 349.00, 20, 'Mystery & Thriller', 'Anchor Books', 2, 5, TRUE),
        ('And Then There Were None', '9780062073488', 'Agatha Christie''s classic mystery strands ten strangers on an island where a deadly pattern begins to unfold.', 249.00, 27, 'Mystery & Thriller', 'William Morrow', 1, 4, TRUE),
        ('The Woman in Cabin 10', '9781501132957', 'Ruth Ware''s suspense novel follows a travel journalist who believes she witnessed a crime aboard a luxury cruise ship.', 349.00, 16, 'Mystery & Thriller', 'Gallery Books', 2, 5, TRUE),
        ('Big Little Lies', '9780399587207', 'Liane Moriarty blends domestic drama, secrets, and mystery around a group of families whose lives collide.', 349.00, 18, 'Mystery & Thriller', 'Berkley', 2, 5, TRUE),
        ('The Guest List', '9780062868930', 'Lucy Foley''s locked-room mystery unfolds at a remote island wedding where old resentments and hidden motives surface.', 349.00, 17, 'Mystery & Thriller', 'William Morrow', 2, 5, TRUE),
        ('A Brief History of Time', '9780553380163', 'Stephen Hawking introduces major questions in cosmology, including space, time, black holes, and the origin of the universe.', 449.00, 15, 'Science', 'Bantam Books', 2, 5, TRUE),
        ('Cosmos', '9780345539434', 'Carl Sagan explores astronomy, scientific discovery, human curiosity, and our place in the universe.', 499.00, 17, 'Science', 'Ballantine Books', 2, 5, TRUE),
        ('Astrophysics for People in a Hurry', '9780393609394', 'Neil deGrasse Tyson offers a compact introduction to the essential ideas shaping modern astrophysics.', 349.00, 20, 'Science', 'W. W. Norton', 2, 4, TRUE),
        ('The Gene', '9781476733524', 'Siddhartha Mukherjee traces the history and science of heredity while examining the human implications of genetics.', 549.00, 13, 'Science', 'Scribner', 3, 6, TRUE),
        ('The Selfish Gene', '9780198788607', 'Richard Dawkins presents an influential gene-centered perspective on evolution and natural selection.', 449.00, 14, 'Science', 'Oxford University Press', 3, 6, TRUE),
        ('The Body', '9780385539302', 'Bill Bryson tours the human body with accessible explanations of anatomy, medicine, and the systems that keep us alive.', 499.00, 16, 'Science', 'Doubleday', 2, 5, TRUE),
        ('The Immortal Life of Henrietta Lacks', '9781400052189', 'Rebecca Skloot explores the scientific legacy of HeLa cells alongside the life and family of Henrietta Lacks.', 449.00, 18, 'Science', 'Broadway Books', 2, 5, TRUE),
        ('Brief Answers to the Big Questions', '9781984819192', 'Stephen Hawking addresses major questions about science, humanity, artificial intelligence, space, and the future.', 449.00, 12, 'Science', 'Bantam Books', 2, 5, TRUE),
        ('Sapiens', '9780062316097', 'Yuval Noah Harari surveys the development of Homo sapiens from early human societies to the modern world.', 549.00, 24, 'History', 'HarperCollins', 2, 5, TRUE),
        ('The Silk Roads', '9781101912379', 'Peter Frankopan reframes world history through the trade routes, cultures, and empires that connected East and West.', 499.00, 14, 'History', 'Vintage Books', 3, 6, TRUE),
        ('Guns, Germs, and Steel', '9780393354324', 'Jared Diamond examines environmental and geographic factors that influenced the development of societies across continents.', 499.00, 12, 'History', 'W. W. Norton', 3, 6, TRUE),
        ('A People''s History of the United States', '9780062397348', 'Howard Zinn presents American history through the experiences of workers, Indigenous peoples, women, and other groups often left outside traditional narratives.', 499.00, 11, 'History', 'Harper Perennial', 3, 6, TRUE),
        ('The Wright Brothers', '9781476728759', 'David McCullough recounts the lives, experiments, determination, and achievements of aviation pioneers Wilbur and Orville Wright.', 449.00, 13, 'History', 'Simon & Schuster', 2, 5, TRUE),
        ('1776', '9780743226721', 'David McCullough tells the story of a pivotal year in the American Revolution through leaders, soldiers, setbacks, and decisive moments.', 449.00, 12, 'History', 'Simon & Schuster', 2, 5, TRUE),
        ('SPQR', '9781631492228', 'Mary Beard offers a broad history of ancient Rome, examining politics, citizenship, empire, and everyday life.', 499.00, 14, 'History', 'Liveright', 3, 6, TRUE),
        ('The Rise and Fall of the Third Reich', '9781451651683', 'William L. Shirer''s extensive historical account examines Nazi Germany from its emergence through its defeat.', 599.00, 9, 'History', 'Simon & Schuster', 3, 7, TRUE),
        ('Atomic Habits', '9780735211292', 'James Clear presents a practical framework for building useful habits and improving through small, repeatable changes.', 449.00, 30, 'Self-Help', 'Avery', 1, 4, TRUE),
        ('Deep Work', '9781455586691', 'Cal Newport explores focused, distraction-free work as a skill for producing valuable results in a noisy world.', 399.00, 22, 'Self-Help', 'Grand Central Publishing', 2, 5, TRUE),
        ('The 7 Habits of Highly Effective People', '9781982137274', 'Stephen R. Covey presents a principle-centered framework for personal effectiveness, relationships, and purposeful work.', 399.00, 19, 'Self-Help', 'Simon & Schuster', 2, 5, TRUE),
        ('How to Win Friends and Influence People', '9780671027032', 'Dale Carnegie''s classic guide focuses on communication, relationships, persuasion, and working effectively with people.', 349.00, 24, 'Self-Help', 'Pocket Books', 2, 4, TRUE),
        ('The Power of Habit', '9780812981605', 'Charles Duhigg explores how habits form, how they influence behavior, and how understanding their structure can support change.', 399.00, 18, 'Self-Help', 'Random House', 2, 5, TRUE),
        ('Mindset', '9780345472328', 'Carol S. Dweck explores fixed and growth mindsets and how beliefs about ability can shape learning, resilience, and achievement.', 399.00, 17, 'Self-Help', 'Ballantine Books', 2, 5, TRUE),
        ('Essentialism', '9780804137386', 'Greg McKeown presents a disciplined approach to identifying what matters most and eliminating work that adds little value.', 449.00, 19, 'Self-Help', 'Crown Publishing', 2, 5, TRUE),
        ('The Subtle Art of Not Giving a F*ck', '9780062457714', 'Mark Manson offers a direct approach to values, responsibility, limits, and choosing what deserves attention.', 399.00, 20, 'Self-Help', 'HarperOne', 2, 5, TRUE),
        ('Zero to One', '9780804139298', 'Peter Thiel and Blake Masters discuss startups, innovation, competition, and creating businesses that deliver genuinely new value.', 449.00, 17, 'Business', 'Crown Business', 2, 5, TRUE),
        ('Good to Great', '9780066620992', 'Jim Collins examines patterns associated with organizations that made sustained transitions from good performance to exceptional results.', 549.00, 16, 'Business', 'HarperBusiness', 2, 5, TRUE),
        ('The Lean Startup', '9780307887894', 'Eric Ries presents an iterative approach to entrepreneurship using validated learning, experimentation, and rapid feedback.', 499.00, 20, 'Business', 'Crown Business', 2, 5, TRUE),
        ('Start with Why', '9781591846444', 'Simon Sinek explores how leaders and organizations can inspire action by communicating a clear sense of purpose.', 449.00, 18, 'Business', 'Portfolio', 2, 5, TRUE),
        ('Measure What Matters', '9780525536222', 'John Doerr explains objectives and key results through examples of organizations using measurable goals to drive focus and alignment.', 499.00, 15, 'Business', 'Portfolio', 2, 5, TRUE),
        ('The Hard Thing About Hard Things', '9780062273208', 'Ben Horowitz shares lessons from building and leading technology companies through difficult decisions and uncertain situations.', 499.00, 14, 'Business', 'HarperBusiness', 2, 5, TRUE),
        ('Shoe Dog', '9781501135927', 'Phil Knight recounts the early years, risks, setbacks, and growth of the company that became Nike.', 449.00, 18, 'Business', 'Scribner', 2, 5, TRUE),
        ('The Innovator''s Dilemma', '9781633691780', 'Clayton Christensen examines why successful companies can struggle with disruptive innovation and changing markets.', 549.00, 12, 'Business', 'Harvard Business Review Press', 3, 6, TRUE)
),
resolved_products AS (
    SELECT
        sp.title,
        sp.isbn,
        sp.description,
        sp.price,
        sp.stock_quantity,
        c.id AS category_id,
        b.id AS brand_id,
        sp.delivery_days_min,
        sp.delivery_days_max,
        sp.active,
        'https://covers.openlibrary.org/b/isbn/'
            || sp.isbn
            || '-L.jpg?default=false' AS image_url
    FROM seed_products sp
    JOIN categories c
      ON c.name = sp.category_name
    JOIN brands b
      ON b.name = sp.brand_name
)
INSERT INTO products (
    title,
    isbn,
    description,
    price,
    stock_quantity,
    category_id,
    brand_id,
    delivery_days_min,
    delivery_days_max,
    active,
    image_url,
    created_at,
    updated_at
)
SELECT
    title,
    isbn,
    description,
    price,
    stock_quantity,
    category_id,
    brand_id,
    delivery_days_min,
    delivery_days_max,
    active,
    image_url,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM resolved_products
ON CONFLICT (isbn) DO UPDATE
SET
    title = EXCLUDED.title,
    description = EXCLUDED.description,
    price = EXCLUDED.price,
    stock_quantity = EXCLUDED.stock_quantity,
    category_id = EXCLUDED.category_id,
    brand_id = EXCLUDED.brand_id,
    delivery_days_min = EXCLUDED.delivery_days_min,
    delivery_days_max = EXCLUDED.delivery_days_max,
    active = EXCLUDED.active,
    image_url = EXCLUDED.image_url,
    updated_at = CURRENT_TIMESTAMP;

COMMIT;

-- =============================================================================
-- Verification
-- =============================================================================

-- Expected seeded_books: 70
SELECT COUNT(*) AS seeded_books
FROM products
WHERE isbn IN (
    '9780132350884',
    '9780134685991',
    '9781492078005',
    '9780321349606',
    '9781617297571',
    '9781718502703',
    '9781449373320',
    '9780135957059',
    '9780134757599',
    '9781492051725',
    '9780134494166',
    '9780321125217',
    '9780451524935',
    '9780451526342',
    '9780743273565',
    '9780061120084',
    '9780062315007',
    '9780553418026',
    '9780375842207',
    '9781594631931',
    '9780147514011',
    '9780141439518',
    '9780547928227',
    '9780547928210',
    '9780590353427',
    '9780553593716',
    '9780756404741',
    '9780765350381',
    '9780765365279',
    '9780064471046',
    '9780307588371',
    '9780307454546',
    '9781250301697',
    '9780307474278',
    '9780062073488',
    '9781501132957',
    '9780399587207',
    '9780062868930',
    '9780553380163',
    '9780345539434',
    '9780393609394',
    '9781476733524',
    '9780198788607',
    '9780385539302',
    '9781400052189',
    '9781984819192',
    '9780062316097',
    '9781101912379',
    '9780393354324',
    '9780062397348',
    '9781476728759',
    '9780743226721',
    '9781631492228',
    '9781451651683',
    '9780735211292',
    '9781455586691',
    '9781982137274',
    '9780671027032',
    '9780812981605',
    '9780345472328',
    '9780804137386',
    '9780062457714',
    '9780804139298',
    '9780066620992',
    '9780307887894',
    '9781591846444',
    '9780525536222',
    '9780062273208',
    '9781501135927',
    '9781633691780'
);

-- Category distribution for this seed only
SELECT
    c.name AS category,
    COUNT(*) AS book_count
FROM products p
JOIN categories c
  ON c.id = p.category_id
WHERE p.isbn IN (
    '9780132350884',
    '9780134685991',
    '9781492078005',
    '9780321349606',
    '9781617297571',
    '9781718502703',
    '9781449373320',
    '9780135957059',
    '9780134757599',
    '9781492051725',
    '9780134494166',
    '9780321125217',
    '9780451524935',
    '9780451526342',
    '9780743273565',
    '9780061120084',
    '9780062315007',
    '9780553418026',
    '9780375842207',
    '9781594631931',
    '9780147514011',
    '9780141439518',
    '9780547928227',
    '9780547928210',
    '9780590353427',
    '9780553593716',
    '9780756404741',
    '9780765350381',
    '9780765365279',
    '9780064471046',
    '9780307588371',
    '9780307454546',
    '9781250301697',
    '9780307474278',
    '9780062073488',
    '9781501132957',
    '9780399587207',
    '9780062868930',
    '9780553380163',
    '9780345539434',
    '9780393609394',
    '9781476733524',
    '9780198788607',
    '9780385539302',
    '9781400052189',
    '9781984819192',
    '9780062316097',
    '9781101912379',
    '9780393354324',
    '9780062397348',
    '9781476728759',
    '9780743226721',
    '9781631492228',
    '9781451651683',
    '9780735211292',
    '9781455586691',
    '9781982137274',
    '9780671027032',
    '9780812981605',
    '9780345472328',
    '9780804137386',
    '9780062457714',
    '9780804139298',
    '9780066620992',
    '9780307887894',
    '9781591846444',
    '9780525536222',
    '9780062273208',
    '9781501135927',
    '9781633691780'
)
GROUP BY c.name
ORDER BY c.name;

-- Expected books_with_image_url: 70
SELECT
    COUNT(*) AS books_with_image_url
FROM products
WHERE isbn IN (
    '9780132350884',
    '9780134685991',
    '9781492078005',
    '9780321349606',
    '9781617297571',
    '9781718502703',
    '9781449373320',
    '9780135957059',
    '9780134757599',
    '9781492051725',
    '9780134494166',
    '9780321125217',
    '9780451524935',
    '9780451526342',
    '9780743273565',
    '9780061120084',
    '9780062315007',
    '9780553418026',
    '9780375842207',
    '9781594631931',
    '9780147514011',
    '9780141439518',
    '9780547928227',
    '9780547928210',
    '9780590353427',
    '9780553593716',
    '9780756404741',
    '9780765350381',
    '9780765365279',
    '9780064471046',
    '9780307588371',
    '9780307454546',
    '9781250301697',
    '9780307474278',
    '9780062073488',
    '9781501132957',
    '9780399587207',
    '9780062868930',
    '9780553380163',
    '9780345539434',
    '9780393609394',
    '9781476733524',
    '9780198788607',
    '9780385539302',
    '9781400052189',
    '9781984819192',
    '9780062316097',
    '9781101912379',
    '9780393354324',
    '9780062397348',
    '9781476728759',
    '9780743226721',
    '9781631492228',
    '9781451651683',
    '9780735211292',
    '9781455586691',
    '9781982137274',
    '9780671027032',
    '9780812981605',
    '9780345472328',
    '9780804137386',
    '9780062457714',
    '9780804139298',
    '9780066620992',
    '9780307887894',
    '9781591846444',
    '9780525536222',
    '9780062273208',
    '9781501135927',
    '9781633691780'
)
  AND image_url IS NOT NULL;

-- Sample rows for quick inspection
SELECT
    p.id,
    p.title,
    p.isbn,
    c.name AS category,
    b.name AS publisher,
    p.price,
    p.stock_quantity,
    p.image_url
FROM products p
JOIN categories c
  ON c.id = p.category_id
JOIN brands b
  ON b.id = p.brand_id
WHERE p.isbn IN (
    '9780132350884',
    '9780134685991',
    '9781492078005',
    '9780321349606',
    '9781617297571',
    '9781718502703',
    '9781449373320',
    '9780135957059',
    '9780134757599',
    '9781492051725',
    '9780134494166',
    '9780321125217',
    '9780451524935',
    '9780451526342',
    '9780743273565',
    '9780061120084',
    '9780062315007',
    '9780553418026',
    '9780375842207',
    '9781594631931',
    '9780147514011',
    '9780141439518',
    '9780547928227',
    '9780547928210',
    '9780590353427',
    '9780553593716',
    '9780756404741',
    '9780765350381',
    '9780765365279',
    '9780064471046',
    '9780307588371',
    '9780307454546',
    '9781250301697',
    '9780307474278',
    '9780062073488',
    '9781501132957',
    '9780399587207',
    '9780062868930',
    '9780553380163',
    '9780345539434',
    '9780393609394',
    '9781476733524',
    '9780198788607',
    '9780385539302',
    '9781400052189',
    '9781984819192',
    '9780062316097',
    '9781101912379',
    '9780393354324',
    '9780062397348',
    '9781476728759',
    '9780743226721',
    '9781631492228',
    '9781451651683',
    '9780735211292',
    '9781455586691',
    '9781982137274',
    '9780671027032',
    '9780812981605',
    '9780345472328',
    '9780804137386',
    '9780062457714',
    '9780804139298',
    '9780066620992',
    '9780307887894',
    '9781591846444',
    '9780525536222',
    '9780062273208',
    '9781501135927',
    '9781633691780'
)
ORDER BY c.name, p.title
LIMIT 20;
