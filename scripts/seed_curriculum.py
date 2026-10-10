#!/usr/bin/env python3
"""
seed_curriculum.py
Generates comprehensive pedagogical grammar rules (.md) and rich lexical datasets (.json)
for all CEFR syllabus topics in Lingua Optima, stored under data/curriculum/rules and data/curriculum/vocabulary.
"""

import os
import json
import re

CURRICULUM_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "data", "curriculum"))
RULES_DIR = os.path.join(CURRICULUM_DIR, "rules")
VOCAB_DIR = os.path.join(CURRICULUM_DIR, "vocabulary")

os.makedirs(RULES_DIR, exist_ok=True)
os.makedirs(VOCAB_DIR, exist_ok=True)

def to_slug(title: str) -> str:
    cleaned = re.sub(r"[^a-zA-Z0-9]+", "_", title.lower())
    return re.sub(r"^_+|_+$", "", cleaned)

TOPICS_DATA = [
    # ==================== CEFR A1 ====================
    {
        "title": "Present Simple (to be & common verbs)",
        "level": "A1",
        "rule": """# Present Simple: 'To Be' and High-Frequency Action Verbs (CEFR A1)

## 1. Pedagogical Overview & Core Objective
The Present Simple is the foundational tense used to express timeless facts, permanent or long-term states, daily routines, and habitual actions. Learners must distinguish between the copular verb *to be* (which acts as its own auxiliary) and lexical action verbs (which require the dummy operator *do / does* in negatives and interrogatives).

## 2. Syntactic Formulas & Conjugation Patterns

### A. The Verb 'To Be'
- **Affirmative**: Subject + `am / is / are` + complement.
  - *I am a student.* / *She is ready.* / *They are at home.*
- **Negative**: Subject + `am not / is not (isn't) / are not (aren't)`.
- **Interrogative**: `Am / Is / Are` + Subject + complement?
  - *Are you happy?* / *Is he the teacher?*

### B. Standard Lexical Action Verbs
- **Affirmative (3rd person singular -s/-es rule)**:
  - Base form for *I / you / we / they*: `Subject + V(base)`. (*They live in Berlin.*)
  - 3rd Person Singular (*he / she / it*): `Subject + V(base) + -s / -es / -ies`.
    - Ending in consonant + y: *study -> studies*, *fly -> flies*.
    - Ending in sibilant (*-s, -sh, -ch, -x, -z*): *watch -> watches*, *fix -> fixes*.
- **Negative**:
  - *I / You / We / They*: `Subject + do not (don't) + V(base)`.
  - *He / She / It*: `Subject + does not (doesn't) + V(base)`. *(Never add -s to the main verb after doesn't!)*
- **Interrogative**:
  - `Do + (I / you / we / they) + V(base)?`
  - `Does + (he / she / it) + V(base)?`

## 3. High-Frequency Adverbs of Frequency & Placement
- *always (100%), usually (80%), often (60%), sometimes (40%), rarely / seldom (10%), never (0%)*.
- **Rule**: Place adverbs of frequency **before main verbs**, but **after the verb 'to be'**.
  - *She **always wakes** up early.*
  - *He **is always** punctual.*

## 4. Common Learner Pitfalls & Diagnostic Checks
- **Error**: *He don't like coffee.* -> **Correction**: *He **doesn't** like coffee.*
- **Error**: *Does she speaks French?* -> **Correction**: *Does she **speak** French?* (double-marking 3rd person).
- **Error**: *I am have a car.* -> **Correction**: *I **have** a car.* (unwarranted auxiliary insertion).
""",
        "vocabulary": [
            {"word": "habitual", "partOfSpeech": "adjective", "definition": "Done by habit, regular or routine.", "cefrLevel": "A1", "collocations": ["habitual routine", "habitual action"], "exampleSentence": "Drinking a glass of water every morning is a habitual routine."},
            {"word": "schedule", "partOfSpeech": "noun", "definition": "A plan that lists events or duties with times.", "cefrLevel": "A1", "collocations": ["daily schedule", "tight schedule"], "exampleSentence": "My daily schedule starts at eight in the morning."},
            {"word": "punctual", "partOfSpeech": "adjective", "definition": "Arriving or doing things at the correct planned time.", "cefrLevel": "A1", "collocations": ["punctual student", "be punctual"], "exampleSentence": "She is always punctual for her morning English lesson."},
            {"word": "frequently", "partOfSpeech": "adverb", "definition": "Happening often or repeatedly.", "cefrLevel": "A1", "collocations": ["visit frequently", "frequently ask"], "exampleSentence": "They frequently eat breakfast together on weekends."},
            {"word": "commute", "partOfSpeech": "verb", "definition": "To travel regularly between one's home and place of work.", "cefrLevel": "A1", "collocations": ["commute to work", "commute by bus"], "exampleSentence": "He commutes to the university by train every weekday."}
        ]
    },
    {
        "title": "Articles (a, an, the) & Demonstratives",
        "level": "A1",
        "rule": """# Definite & Indefinite Articles and Demonstratives (CEFR A1)

## 1. Pedagogical Overview
Articles determine the specificity of nouns in English. Learners must grasp the contrast between generic/first-mention entities (*a/an*) and uniquely identified, known, or second-mention entities (*the*), alongside zero article (*Ø*) for plural and uncountable generalizations. Demonstratives locate entities in space and time.

## 2. Core Rules & Syntactic Patterns

### A. Indefinite Article: 'a' vs 'an'
- Applied only to **singular countable nouns**.
- Determined by the **initial phonetic sound**, NOT spelling:
  - *a university* (/juː/ consonant sound) vs *an umbrella* (/ʌ/ vowel sound).
  - *a European country* (/j/) vs *an honest person* (/ɒ/ silent h).
- Function: Introducing an entity for the first time, categorizing professions (*She is an architect*).

### B. Definite Article: 'the'
- Applied to singular, plural, and uncountable nouns when both speaker and listener know the referent.
- Triggers:
  - Uniqueness in the universe (*the sun, the internet, the sky*).
  - Second mention (*I bought a book. The book was fascinating.*).
  - Superlatives and ordinals (*the tallest building, the first attempt*).
  - Specific predefined groups (*the students in room 302*).

### C. Zero Article (Ø)
- Generalizations with plural countable nouns (*Dogs are loyal animals*).
- Generalizations with abstract/uncountable nouns (*Knowledge is power*).
- Most singular countries, towns, lakes, meals, sports (*in Italy, at dinner, plays tennis*).

### D. Demonstrative Determiners & Pronouns
- Singular / Near: **this** (*this pen here*)
- Plural / Near: **these** (*these books here*)
- Singular / Distant: **that** (*that car over there*)
- Plural / Distant: **those** (*those birds in the tree*)
""",
        "vocabulary": [
            {"word": "identifier", "partOfSpeech": "noun", "definition": "A word or symbol that points out a specific identity.", "cefrLevel": "A1", "collocations": ["unique identifier", "determiner"], "exampleSentence": "The definite article acts as a specific identifier."},
            {"word": "countable", "partOfSpeech": "adjective", "definition": "Capable of being counted individually.", "cefrLevel": "A1", "collocations": ["countable noun", "countable entity"], "exampleSentence": "Apples and pens are countable nouns in English."},
            {"word": "demonstrative", "partOfSpeech": "adjective", "definition": "Indicating or pointing out directly in space or time.", "cefrLevel": "A1", "collocations": ["demonstrative pronoun", "demonstrative adjective"], "exampleSentence": "Use 'these' as a demonstrative for multiple items close to you."}
        ]
    },
    {
        "title": "Basic Prepositions of Place & Time (in, at, on)",
        "level": "A1",
        "rule": """# Spatial & Temporal Prepositions: In, On, At (CEFR A1)

## 1. Geometric & Temporal Hierarchy (Pyramid Model)
Prepositions of time and place follow an inverted pyramid model: from general/large (IN) to medium/surface (ON) to specific/precise (AT).

## 2. Prepositions of Time
- **IN (General, Extended Periods)**:
  - Centuries, decades, years, months (*in the 21st century, in 1995, in October*).
  - Seasons (*in summer, in winter*).
  - Parts of the day (*in the morning, in the evening* - exception: *at night*).
- **ON (Specific Days & Dates)**:
  - Days of the week (*on Monday, on Sundays*).
  - Calendar dates (*on October 14th, on New Year's Day*).
  - Day + part of day (*on Friday evening*).
- **AT (Exact Clock Time & Points in Time)**:
  - Exact hours (*at 7:30 PM, at noon, at midnight*).
  - Festive periods (*at Christmas, at Easter* - as holidays, vs *on Christmas Day*).
  - Set expressions (*at the weekend, at present, at the moment*).

## 3. Prepositions of Place
- **IN (3D Enclosed Spaces, Cities, Countries, Volumes)**:
  - *in the classroom, in London, in a box, in the car, in the swimming pool*.
- **ON (2D Surfaces, Lines, Public Transit Fleets)**:
  - *on the table, on the wall, on the 3rd floor, on the train, on the bus, on a plane*.
- **AT (Specific Exact Points, Addresses with Numbers, Institutional Purposes)**:
  - *at the bus stop, at 42 Baker Street, at the door, at school, at university, at work*.
""",
        "vocabulary": [
            {"word": "enclosed", "partOfSpeech": "adjective", "definition": "Surrounded on all sides by boundaries or walls.", "cefrLevel": "A1", "collocations": ["enclosed space", "enclosed area"], "exampleSentence": "Preposition 'in' refers to enclosed containers or rooms."},
            {"word": "boundary", "partOfSpeech": "noun", "definition": "A real or imagined line that marks the edge or limit of an area.", "cefrLevel": "A1", "collocations": ["city boundary", "spatial boundary"], "exampleSentence": "The park boundary is clearly marked on the campus map."},
            {"word": "intersection", "partOfSpeech": "noun", "definition": "A point where two roads meet and cross.", "cefrLevel": "A1", "collocations": ["busy intersection", "wait at the intersection"], "exampleSentence": "The bus stops right at the intersection."}
        ]
    },
    {
        "title": "Can / Can't for Ability & Permission",
        "level": "A1",
        "rule": """# Modal Verb 'Can' & 'Cannot' (Ability, Permission, Possibility) (CEFR A1)

## 1. Grammatical Characteristics
- Modal auxiliary verbs have no -s in the 3rd person singular (*he can*, never *he cans*).
- Directly followed by the **bare infinitive** (base form without *to*).
- Invert for questions (*Can you swim?*), add *not* for negatives (*cannot / can't*).

## 2. Core Functions
1. **Physical or Learned Ability**:
   - *I can speak three languages.*
   - *She can play the violin beautifully.*
2. **Permission (Informal)**:
   - *Can I borrow your pencil?* - *Yes, you can.*
   - *You can't park here; it's a private lot.*
3. **General Theoretical Possibility**:
   - *English spelling can be tricky for beginners.*
""",
        "vocabulary": [
            {"word": "capability", "partOfSpeech": "noun", "definition": "The power or ability to do something.", "cefrLevel": "A1", "collocations": ["demonstrate capability", "physical capability"], "exampleSentence": "He has the capability to solve complex math problems quickly."},
            {"word": "permission", "partOfSpeech": "noun", "definition": "Official authorization or agreement to do something.", "cefrLevel": "A1", "collocations": ["ask for permission", "grant permission"], "exampleSentence": "You must ask for permission before leaving the exam room."}
        ]
    },
    {
        "title": "Possessive Adjectives & Possessive 's",
        "level": "A1",
        "rule": """# Possession in English: Adjectives, Pronouns, and Genitive 's (CEFR A1)

## 1. Possessive Determiners (Adjectives) vs Possessive Pronouns
- Determiners precede nouns: `my, your, his, her, its, our, their + noun`.
  - *This is **my laptop**.*
- Pronouns replace the noun phrase: `mine, yours, his, hers, ours, theirs`.
  - *This laptop is **mine**.* (Never *This laptop is my*).

## 2. The Saxon Genitive ('s)
- Singular noun: add `'s` (*the student's assignment, Charles's coat*).
- Regular plural ending in -s: add `'` only (*the students' assignments, the teachers' lounge*).
- Irregular plural not ending in -s: add `'s` (*children's books, people's rights*).
- Joint possession: *Tom and Mary's house* (one house shared).
- Separate possession: *Tom's and Mary's houses* (two distinct houses).
""",
        "vocabulary": [
            {"word": "belonging", "partOfSpeech": "noun", "definition": "Items owned by someone; property.", "cefrLevel": "A1", "collocations": ["personal belongings", "sense of belonging"], "exampleSentence": "Please ensure you take all personal belongings with you."},
            {"word": "possession", "partOfSpeech": "noun", "definition": "The state of having, owning, or controlling something.", "cefrLevel": "A1", "collocations": ["take possession", "in possession of"], "exampleSentence": "The dictionary is in her possession."}
        ]
    },
    {
        "title": "Imperatives & Basic Question Formation",
        "level": "A1",
        "rule": """# Imperative Directives and Canonical Question Formation (CEFR A1)

## 1. The Imperative Mood
- Addressed directly to the listener (*you* implied).
- **Affirmative**: Base form of verb with zero subject. (*Open your books. Listen carefully.*)
- **Negative**: `Do not / Don't + base verb`. (*Don't touch that button.*)
- **Polite softenings**: *Please take a seat.* / *Could you please pass the water?*

## 2. The QUASM Framework for Information Questions
All standard information questions in English adhere to the strict **QUASM** structural order:
- **QU**estion word: *What, Where, When, Why, How, Who*
- **A**uxiliary verb: *do, does, did, is, are, have, can*
- **S**ubject: *you, she, the train, Mr. Davis*
- **M**ain verb: *live, want, arrive, study*
  - Example: `Where (QU) do (A) you (S) live (M)?`
  - Example: `Why (QU) did (A) they (S) leave (M)?`
""",
        "vocabulary": [
            {"word": "directive", "partOfSpeech": "noun", "definition": "An official instruction or authoritative order.", "cefrLevel": "A1", "collocations": ["follow a directive", "clear directive"], "exampleSentence": "The teacher gave a clear directive to open page forty."},
            {"word": "auxiliary", "partOfSpeech": "adjective", "definition": "Providing supplementary or additional help and support.", "cefrLevel": "A1", "collocations": ["auxiliary verb", "auxiliary support"], "exampleSentence": "'Do' is the auxiliary verb used to build the question."}
        ]
    },

    # ==================== CEFR A2 ====================
    {
        "title": "Past Simple (Regular & Irregular Verbs)",
        "level": "A2",
        "rule": """# Past Simple Tense: Completed Historical Actions (CEFR A2)

## 1. Pedagogical Scope & Meaning
The Past Simple expresses completed events, states, and sequential actions anchored to a definite time in the past that has completely concluded.

## 2. Morphological Forms
- **Regular Verbs**: V(base) + `-ed` (*walk -> walked, decide -> decided*).
  - Spelling: consonant + y -> -ied (*try -> tried*), CVC doubling (*stop -> stopped*).
- **Irregular Verbs**: Memorized historical ablaut forms (*go -> went, see -> saw, buy -> bought, take -> took*).
- **Negative**: `Subject + did not (didn't) + V(base)`. (*She didn't visit Rome.* - never *didn't visited*).
- **Interrogative**: `Did + Subject + V(base)?` (*Did you finish your homework?*).

## 3. Definite Time Markers (Mandatory Anchors)
- *yesterday, last night, last week, two years ago, in 2014, when I was ten, at that moment*.
""",
        "vocabulary": [
            {"word": "sequential", "partOfSpeech": "adjective", "definition": "Forming or following a logical chronological order.", "cefrLevel": "A2", "collocations": ["sequential events", "sequential order"], "exampleSentence": "The witness described the sequential events of yesterday evening."},
            {"word": "historical", "partOfSpeech": "adjective", "definition": "Belonging to the past; of or concerning history.", "cefrLevel": "A2", "collocations": ["historical fact", "historical context"], "exampleSentence": "The novel is based on historical facts from the eighteenth century."},
            {"word": "conclude", "partOfSpeech": "verb", "definition": "To bring or come to an end; finish.", "cefrLevel": "A2", "collocations": ["conclude a meeting", "successfully conclude"], "exampleSentence": "The conference concluded yesterday at five o'clock."}
        ]
    },
    {
        "title": "Future with 'Going to' vs 'Will'",
        "level": "A2",
        "rule": """# Expressing Future Intentions: 'Be Going To' vs 'Will' (CEFR A2)

## 1. Core Distinction: Prior Plan vs Spontaneous Decision
- **Be Going To**:
  - Pre-meditated intentions and plans made *before* the moment of speech. (*I am going to study medicine next autumn.*)
  - Predictions based on immediate sensory evidence in the present. (*Look at those black clouds! It is going to rain.*)
- **Will (Future Simple)**:
  - Spontaneous decisions made *at* the moment of speaking. (*The phone is ringing. I'll get it!*)
  - Offers, promises, and threats (*I will help you with your bags.* / *I promise I will call.*)
  - Objective forecasts and opinions (*I think prices will rise next month.*)

## 2. Syntactic Formulas
- `Subject + am/is/are + going to + V(base)`
- `Subject + will / won't + V(base)`
""",
        "vocabulary": [
            {"word": "premeditated", "partOfSpeech": "adjective", "definition": "Considered or planned beforehand.", "cefrLevel": "A2", "collocations": ["premeditated plan", "premeditated action"], "exampleSentence": "Traveling to Oxford was a premeditated decision made months ago."},
            {"word": "spontaneous", "partOfSpeech": "adjective", "definition": "Performed or occurring as a result of a sudden impulse without premeditation.", "cefrLevel": "A2", "collocations": ["spontaneous decision", "spontaneous reaction"], "exampleSentence": "Ordering dessert was a spontaneous decision at the table."},
            {"word": "evidence", "partOfSpeech": "noun", "definition": "Facts or signs that indicate whether something is true or valid.", "cefrLevel": "A2", "collocations": ["clear evidence", "visible evidence"], "exampleSentence": "Dark clouds provide visible evidence that it is going to rain."}
        ]
    },
    {
        "title": "Comparative and Superlative Adjectives",
        "level": "A2",
        "rule": """# Comparative and Superlative Grading of Adjectives (CEFR A2)

## 1. Structural Rules by Syllable Count
- **1-syllable**: `-er / -est` (*fast -> faster than -> the fastest*).
- **2-syllables ending in -y**: `-ier / -iest` (*busy -> busier than -> the busiest*).
- **2+ syllables**: `more / most` (*expensive -> more expensive than -> the most expensive*).
- **Irregular Forms**:
  - *good -> better -> the best*
  - *bad -> worse -> the worst*
  - *far -> further / farther -> the furthest / farthest*

## 2. Equative Structures
- `as + adjective + as`: equality (*She is as tall as her brother.*)
- `not as / not so + adjective + as`: lesser degree (*London is not as hot as Madrid.*)
""",
        "vocabulary": [
            {"word": "dimension", "partOfSpeech": "noun", "definition": "A measurable extent of some kind, such as length, breadth, depth, or height.", "cefrLevel": "A2", "collocations": ["measure dimensions", "key dimension"], "exampleSentence": "Weight is one comparative dimension between the two packages."},
            {"word": "superior", "partOfSpeech": "adjective", "definition": "Higher in rank, status, quality, or achievement.", "cefrLevel": "A2", "collocations": ["superior quality", "vastly superior"], "exampleSentence": "The handmade notebook is of superior quality compared to standard paper."}
        ]
    },
    {
        "title": "Countable vs Uncountable Nouns (some, any, much, many)",
        "level": "A2",
        "rule": """# Quantification: Countable and Uncountable Entities (CEFR A2)

## 1. Countable vs Uncountable Categories
- **Countable**: Distinct individual units that take plural forms (*cups, ideas, apples*).
  - Determiners: *many, few, a few, several, number of*.
- **Uncountable**: Liquids, gases, materials, collective concepts, abstract nouns (*water, information, furniture, luggage, advice, research*).
  - Determiners: *much, little, a little, amount of*.

## 2. Some vs Any Distribution
- **SOME**: Affirmative statements (*I have some questions*), and polite offers/requests (*Would you like some tea?*).
- **ANY**: Negative statements (*We don't have any bread left*) and open inquiries (*Do you have any questions?*).
""",
        "vocabulary": [
            {"word": "quantifier", "partOfSpeech": "noun", "definition": "A word or phrase used before a noun to indicate the amount or quantity.", "cefrLevel": "A2", "collocations": ["indefinite quantifier", "correct quantifier"], "exampleSentence": "'Much' is a quantifier used exclusively with non-count nouns."},
            {"word": "collective", "partOfSpeech": "adjective", "definition": "Done by people acting as a group; taken as a whole.", "cefrLevel": "A2", "collocations": ["collective noun", "collective effort"], "exampleSentence": "Furniture is a collective noun and cannot take an indefinite article."}
        ]
    },
    {
        "title": "Have to & Must (Basic Rules)",
        "level": "A2",
        "rule": """# Modals of Necessity and Obligation: Must vs Have To (CEFR A2)

## 1. Core Distinction in Obligation
- **Must**: Internal obligation originating from the speaker's personal moral conviction or urgency (*I must remember to write down this password*).
- **Have To**: External obligation imposed by external regulations, laws, or circumstances (*Students have to submit essays before Friday*).

## 2. Crucial Asymmetry in Negatives
- **Must not / Musn't = PROHIBITION** (Strictly forbidden).
  - *You must not smoke inside the terminal.*
- **Don't have to = LACK OF OBLIGATION** (Optional, discretionary).
  - *You don't have to dress formally; jeans are fine.*
""",
        "vocabulary": [
            {"word": "obligation", "partOfSpeech": "noun", "definition": "An act or course of action to which a person is morally or legally bound.", "cefrLevel": "A2", "collocations": ["legal obligation", "moral obligation"], "exampleSentence": "Every citizen has a legal obligation to pay their taxes."},
            {"word": "prohibition", "partOfSpeech": "noun", "definition": "The action of forbidding something, especially by law.", "cefrLevel": "A2", "collocations": ["strict prohibition", "impose a prohibition"], "exampleSentence": "There is a strict prohibition against using mobile phones during the test."}
        ]
    },
    {
        "title": "Present Continuous for Future Arrangements",
        "level": "A2",
        "rule": """# Present Continuous for Definite Future Arrangements (CEFR A2)

## 1. Communicative Function
The Present Continuous (`be + V-ing`) denotes fixed personal arrangements and scheduled appointments where time, place, and participants have already been established.

## 2. Contrast with 'Going To'
- *I am going to visit the dentist.* (My internal intention, but appointment might not be booked).
- *I am visiting the dentist tomorrow at 3 PM.* (Fixed calendar appointment, dentist notified).
""",
        "vocabulary": [
            {"word": "arrangement", "partOfSpeech": "noun", "definition": "A plan or preparation for a future event.", "cefrLevel": "A2", "collocations": ["make arrangements", "future arrangement"], "exampleSentence": "We have finalized all arrangements for the international symposium."},
            {"word": "confirmed", "partOfSpeech": "adjective", "definition": "Fixed, settled, or verified definitely.", "cefrLevel": "A2", "collocations": ["confirmed appointment", "confirmed booking"], "exampleSentence": "She has a confirmed booking on Friday evening."}
        ]
    },

    # ==================== CEFR B1 ====================
    {
        "title": "Present Perfect vs Past Simple",
        "level": "B1",
        "rule": """# Present Perfect Aspect vs Past Simple Tense (CEFR B1)

## 1. Core Aspectual Contrast
- **Past Simple**: Completed event situated at a closed, definite time point in the past. Disconnected from the present. (*I lost my passport in Berlin last year, but I found it later.*)
- **Present Perfect**: Bridges the past and the present. Expresses experience, continuous duration up to now, or a recent action whose consequence directly impacts the present moment. (*I have lost my passport! I cannot board the plane now.*)

## 2. Temporal Marker Diagnostic
- **Past Simple**: *yesterday, in 2020, three days ago, when I was young, last month*.
- **Present Perfect**: *already, yet, just, ever, never, since, for, so far, recently, lately*.

## 3. For vs Since
- `Since + starting point in time`: *since 2018, since breakfast, since last Monday*.
- `For + duration of time`: *for ten minutes, for three years, for a decade*.
""",
        "vocabulary": [
            {"word": "aspectual", "partOfSpeech": "adjective", "definition": "Relating to the aspect of a verb denoting time perspective.", "cefrLevel": "B1", "collocations": ["aspectual difference", "aspectual distinction"], "exampleSentence": "The aspectual contrast between perfect and simple tenses is essential."},
            {"word": "consequence", "partOfSpeech": "noun", "definition": "A result or effect, typically one that is unwelcome or important.", "cefrLevel": "B1", "collocations": ["direct consequence", "endure the consequence"], "exampleSentence": "His current success is a direct consequence of years of dedicated study."},
            {"word": "duration", "partOfSpeech": "noun", "definition": "The time during which something continues.", "cefrLevel": "B1", "collocations": ["for the duration of", "short duration"], "exampleSentence": "She stayed in Prague for the duration of the semester."}
        ]
    },
    {
        "title": "Past Continuous",
        "level": "B1",
        "rule": """# Past Continuous: Ongoing Past Actions and Background Narration (CEFR B1)

## 1. Syntactic Structure
`Subject + was / were + V-ing`

## 2. Pedagogical Functions
1. **Action in Progress at Specific Past Time**:
   - *At 9:00 PM yesterday, I was studying in the library.*
2. **Interrupted Past Action (Background vs Interruption)**:
   - Long background ongoing action in Past Continuous (`while / as`).
   - Short sharp interrupting action in Past Simple (`when`).
   - *While we were driving to the airport, the tire burst.*
3. **Simultaneous Past Actions**:
   - *While Daniel was preparing dinner, Sarah was setting the table.*
""",
        "vocabulary": [
            {"word": "interruption", "partOfSpeech": "noun", "definition": "An act of stopping the continuous progress of an activity or process.", "cefrLevel": "B1", "collocations": ["without interruption", "sudden interruption"], "exampleSentence": "The alarm sounded as a sudden interruption while they were negotiating."},
            {"word": "simultaneous", "partOfSpeech": "adjective", "definition": "Occurring, operating, or done at the same time.", "cefrLevel": "B1", "collocations": ["simultaneous translation", "simultaneous events"], "exampleSentence": "Both experiments were conducted in simultaneous sessions."}
        ]
    },
    {
        "title": "Conditionals 0, 1, 2",
        "level": "B1",
        "rule": """# Real and Hypothetical Conditionals: Zero, First, and Second (CEFR B1)

## 1. Zero Conditional: Scientific Truths & Invariable Habits
- `If + Present Simple, ... Present Simple`
- *If you heat ice, it melts.* / *If water reaches 100 degrees Celsius, it boils.*

## 2. First Conditional: Real & Probable Future Scenarios
- `If + Present Simple, ... will / can / might / should + V(base)`
- *If it rains tomorrow, we will postpone the match.*
- *Unless you register today, you will lose your seat.* (*Unless = If not*).

## 3. Second Conditional: Hypothetical, Unreal, or Improbable Present/Future
- `If + Past Simple, ... would / could / might + V(base)`
- Subjunctive *were* preferred for all persons: *If I were you, I would consult a lawyer.*
- *If I had a million pounds, I would establish a charitable foundation.*
""",
        "vocabulary": [
            {"word": "hypothetical", "partOfSpeech": "adjective", "definition": "Based on or serving as a hypothesis; imagined or theoretical.", "cefrLevel": "B1", "collocations": ["hypothetical question", "hypothetical scenario"], "exampleSentence": "The professor asked a hypothetical question regarding monetary policy."},
            {"word": "contingency", "partOfSpeech": "noun", "definition": "A future event or circumstance which is possible but cannot be predicted with certainty.", "cefrLevel": "B1", "collocations": ["contingency plan", "prepare for contingencies"], "exampleSentence": "We developed a contingency plan in case the supply chain stalled."},
            {"word": "invariable", "partOfSpeech": "adjective", "definition": "Never changing; constant and predictable.", "cefrLevel": "B1", "collocations": ["invariable law", "invariable outcome"], "exampleSentence": "Gravity is an invariable law of classical physics."}
        ]
    },
    {
        "title": "Passive Voice (Present & Past Simple)",
        "level": "B1",
        "rule": """# The Passive Voice: Agent Demotion and Focalization (CEFR B1)

## 1. Communicative Rationale
Use the passive voice when:
- The actor/agent is unknown, obvious, or irrelevant.
- The action or object is the primary rhetorical focus of the sentence.
- Creating objective, formal, or academic register.

## 2. Syntactic Formulas
- **Present Simple Passive**: `Subject + am / is / are + V3 (Past Participle)`
  - *Coffee is cultivated in tropical regions.*
- **Past Simple Passive**: `Subject + was / were + V3 (Past Participle)`
  - *The telegram was sent in 1912.*
- **Agent Attribution**: Use `by + agent` only when the specific actor adds crucial informative value (*The play was written by Shakespeare*).
""",
        "vocabulary": [
            {"word": "cultivate", "partOfSpeech": "verb", "definition": "To prepare and use land for crops, or nurture a quality.", "cefrLevel": "B1", "collocations": ["cultivate land", "cultivate relations"], "exampleSentence": "Premium arabica beans are cultivated at high altitudes."},
            {"word": "focalization", "partOfSpeech": "noun", "definition": "The act of directing attention or structural focus toward a specific element.", "cefrLevel": "B1", "collocations": ["sentence focalization", "thematic focalization"], "exampleSentence": "Passive constructions enable clear focalization on the research findings."},
            {"word": "attribution", "partOfSpeech": "noun", "definition": "The action of regarding something as being caused by a person or thing.", "cefrLevel": "B1", "collocations": ["proper attribution", "authorial attribution"], "exampleSentence": "The quote requires proper attribution to its original source."}
        ]
    },

    # ==================== CEFR B2 ====================
    {
        "title": "Third Conditional & Mixed Conditionals",
        "level": "B2",
        "rule": """# Third Conditional and Mixed Counterfactuals (CEFR B2)

## 1. Third Conditional (Counterfactual Past -> Counterfactual Past Outcome)
- Formula: `If + Past Perfect, ... would / could / might have + V3`
- *If she had checked the weather forecast, she would not have sailed into the storm.*
- Expresses past regrets, missed opportunities, and historical counter-evidence.

## 2. Mixed Conditionals
- **Type A: Past Action -> Present State/Consequence**:
  - `If + Past Perfect, ... would + V(base)`
  - *If I had accepted that job in London (past), I would be living in Britain today (present).*
- **Type B: Permanent Present State -> Past Outcome**:
  - `If + Past Simple, ... would have + V3`
  - *If he spoke fluent German (permanent ability), he would have translated the contract yesterday (past event).*
""",
        "vocabulary": [
            {"word": "counterfactual", "partOfSpeech": "adjective", "definition": "Relating to or expressing what has not happened or is contrary to actual fact.", "cefrLevel": "B2", "collocations": ["counterfactual reasoning", "counterfactual thinking"], "exampleSentence": "Third conditionals represent pure counterfactual reasoning about the past."},
            {"word": "contingent", "partOfSpeech": "adjective", "definition": "Subject to chance or dependent on circumstances.", "cefrLevel": "B2", "collocations": ["contingent upon", "contingent liability"], "exampleSentence": "The signing of the treaty was contingent upon electoral approval."},
            {"word": "irreversible", "partOfSpeech": "adjective", "definition": "Not able to be undone or altered.", "cefrLevel": "B2", "collocations": ["irreversible damage", "irreversible decision"], "exampleSentence": "The historical mistake led to irreversible ecological consequences."}
        ]
    },
    {
        "title": "Reported Speech",
        "level": "B2",
        "rule": """# Indirect and Reported Discourse: Backshift & Reporting Verbs (CEFR B2)

## 1. Canonical Backshift Paradigm
When the introductory reporting verb is in a past tense (*said, told, announced*), subordinate clauses backshift one step into the past:
- Present Simple -> Past Simple (*'I know' -> He said he knew*).
- Present Continuous -> Past Continuous (*'We are leaving' -> They said they were leaving*).
- Past Simple / Present Perfect -> Past Perfect (*'I arrived / have arrived' -> She said she had arrived*).
- Will -> Would, Can -> Could, May -> Might.

## 2. Advanced Pragmatic Reporting Verbs
Move beyond basic *say / tell* toward nuance:
- `Verb + that`: *admit, explain, claim, deny, insist, suggest*.
- `Verb + to-infinitive`: *offer, promise, refuse, threaten, agree*.
- `Verb + object + to-infinitive`: *warn, advise, encourage, remind, persuade*.
- `Verb + gerund / preposition + gerund`: *apologize for, accuse someone of, congratulate on*.
""",
        "vocabulary": [
            {"word": "backshift", "partOfSpeech": "noun", "definition": "The chronological shifting back of tense forms in indirect speech.", "cefrLevel": "B2", "collocations": ["canonical backshift", "apply backshift"], "exampleSentence": "Learners must apply backshift when the reporting verb is in the past."},
            {"word": "rebuttal", "partOfSpeech": "noun", "definition": "A refutation or contradiction of an argument or statement.", "cefrLevel": "B2", "collocations": ["offer a rebuttal", "deliver a rebuttal"], "exampleSentence": "The spokesperson issued a firm rebuttal against the allegations."},
            {"word": "allegation", "partOfSpeech": "noun", "definition": "A claim or assertion that someone has done something illegal or wrong.", "cefrLevel": "B2", "collocations": ["deny allegations", "unfounded allegation"], "exampleSentence": "The minister strongly denied the financial allegations."}
        ]
    },
    {
        "title": "Inversion with Negative Adverbials",
        "level": "B2",
        "rule": """# Subject-Auxiliary Inversion with Negative and Restrictive Adverbials (CEFR B2)

## 1. Communicative & Rhetorical Effect
Fronting negative, limiting, or restrictive adverbial phrases to sentence-initial position creates heightened dramatic, emphatic, or formal stylistic emphasis.

## 2. Syntactic Formula
`Negative Adverbial Phrase + Auxiliary Verb + Subject + Main Verb`

## 3. High-Frequency Triggers
- **Seldom / Rarely / Never**:
  - *Seldom **have I encountered** such extraordinary dedication.*
- **Hardly / Scarcely ... when**:
  - *Hardly **had we boarded** the train when the doors slammed shut.*
- **No sooner ... than**:
  - *No sooner **had the bell rung** than the students departed.*
- **Under no circumstances / In no way / On no account**:
  - *Under no circumstances **should you disclose** your private credentials.*
- **Not only ... but also**:
  - *Not only **did they meet** the target, but they also exceeded expectations.*
""",
        "vocabulary": [
            {"word": "inversion", "partOfSpeech": "noun", "definition": "The reversal of the normal word order in a sentence for rhetorical effect.", "cefrLevel": "B2", "collocations": ["negative inversion", "subject-auxiliary inversion"], "exampleSentence": "Negative inversion elevates the register of written English essays."},
            {"word": "restrictive", "partOfSpeech": "adjective", "definition": "Imposing limits, constraints, or qualifications on meaning.", "cefrLevel": "B2", "collocations": ["restrictive clause", "restrictive adverbial"], "exampleSentence": "'Hardly' and 'scarcely' are restrictive adverbials that trigger inversion."},
            {"word": "fronting", "partOfSpeech": "noun", "definition": "The syntactic placement of a constituent to the beginning of a clause.", "cefrLevel": "B2", "collocations": ["thematic fronting", "syntactic fronting"], "exampleSentence": "Fronting the negative phrase heightens suspense and academic gravity."}
        ]
    },

    # ==================== CEFR C1 ====================
    {
        "title": "Subjunctive Mood",
        "level": "C1",
        "rule": """# The Mandative Subjunctive and Hypothetical Unreal Forms (CEFR C1)

## 1. The Mandative Subjunctive
Employed in formal, legal, and academic English following matrix clauses denoting demand, recommendation, urgency, or formal resolution.
- Structure: `Matrix verb/adjective + that + Subject + V(base bare infinitive)`.
- **Crucial Rule**: The bare infinitive remains unchanged for all persons, including 3rd person singular (no -s, no past forms).
  - *The committee recommended that he **resign** immediately.* (Not *resigns*).
  - *It is imperative that every candidate **be** seated before 9:00 AM.* (Not *is*).
  - *The judge ordered that the defendant **not leave** the jurisdiction.*

## 2. Trigger Verbs & Adjectives
- **Verbs**: *demand, insist, propose, recommend, request, require, urge, stipulate, decree*.
- **Adjectives**: *vital, crucial, imperative, essential, necessary, desirable*.
""",
        "vocabulary": [
            {"word": "mandative", "partOfSpeech": "adjective", "definition": "Expressing an authoritative command, decree, or imperative.", "cefrLevel": "C1", "collocations": ["mandative subjunctive", "mandative clause"], "exampleSentence": "The mandative subjunctive conveys formal administrative necessity."},
            {"word": "stipulate", "partOfSpeech": "verb", "definition": "To demand or specify a requirement as part of an official bargain.", "cefrLevel": "C1", "collocations": ["stipulate conditions", "clearly stipulate"], "exampleSentence": "The contract stipulates that both parties resolve disputes via arbitration."},
            {"word": "imperative", "partOfSpeech": "adjective", "definition": "Of vital importance; crucial or mandatory.", "cefrLevel": "C1", "collocations": ["categorically imperative", "it is imperative that"], "exampleSentence": "It is imperative that environmental regulations be rigorously enforced."}
        ]
    },
    {
        "title": "Cleft Sentences",
        "level": "C1",
        "rule": """# Cleft Constructions: It-Clefts and Wh-Pseudo-Clefts (CEFR C1)

## 1. Communicative & Information Packaging Function
Clefting divides a single semantic proposition into two clauses to isolate and emphatically highlight a specific constituent (contrastive focus).

## 2. Structural Variants

### A. It-Clefts
- Formula: `It + is / was + [Highlighted Constituent] + that / who + [Rest of Clause]`
  - Standard: *Alexander Fleming discovered penicillin in 1928.*
  - Cleft Focus on Actor: *It was **Alexander Fleming** who discovered penicillin in 1928.*
  - Cleft Focus on Object: *It was **penicillin** that Fleming discovered in 1928.*
  - Cleft Focus on Time: *It was **in 1928** that Fleming discovered penicillin.*

### B. Wh-Pseudo-Clefts
- Formula: `What + clause + is / was + [Focal Element]`
  - *What surprised the analysts was the unprecedented surge in consumer demand.*
  - *All that we require is transparent documentation.*
  - *The person who made the final determination was the Chief Inspector.*
""",
        "vocabulary": [
            {"word": "focalization", "partOfSpeech": "noun", "definition": "The linguistic structuring that directs attention to a focused topic.", "cefrLevel": "C1", "collocations": ["contrastive focalization", "discourse focalization"], "exampleSentence": "It-clefts achieve sharp contrastive focalization in scholarly prose."},
            {"word": "proposition", "partOfSpeech": "noun", "definition": "A statement or assertion that expresses a judgment or opinion.", "cefrLevel": "C1", "collocations": ["underlying proposition", "test a proposition"], "exampleSentence": "The author cleaves the underlying proposition to emphasize the financial motive."},
            {"word": "constituent", "partOfSpeech": "noun", "definition": "A syntactic unit that functions as a component of a larger construction.", "cefrLevel": "C1", "collocations": ["syntactic constituent", "highlighted constituent"], "exampleSentence": "The cleft structure foregrounds the prepositional constituent."}
        ]
    },
    {
        "title": "Participle Clauses",
        "level": "C1",
        "rule": """# Participle Clauses and Reduced Adverbial Structures (CEFR C1)

## 1. Stylistic Utility in Advanced Register
Participle clauses economize prose by condensing subordinate adverbial clauses (time, reason, condition, concession) into concise participial phrases.

## 2. Forms and Functions
- **Present Participle (-ing)**: Active, contemporaneous or immediate causal relation.
  - *Knowing his temper, I decided not to argue.* (= *Because I knew his temper...*)
  - *Walking along the river, she spotted a rare heron.* (= *While she was walking...*)
- **Past Participle (-ed / V3)**: Passive condition or state.
  - *Shocked by the news, the community held an emergency meeting.* (= *Because they were shocked...*)
- **Perfect Participle (Having + V3)**: Emphasizes that one action was fully completed before the next began.
  - *Having completed the rigorous audit, the accountants signed off on the balance sheet.*
  - Passive Perfect: *Having been warned repeatedly, he could offer no excuses.*

## 3. Dangling Participle Caution
The implied subject of the participle clause **must** match the grammatical subject of the main clause:
- Dangling Error: *Arriving at the station, the train had already left.* (Implies the train arrived at the station and left itself).
- Correction: *Arriving at the station, **we discovered** that the train had already left.*
""",
        "vocabulary": [
            {"word": "economize", "partOfSpeech": "verb", "definition": "To use resources or linguistic elements sparingly or efficiently.", "cefrLevel": "C1", "collocations": ["economize prose", "economize on words"], "exampleSentence": "Academic writers use participle clauses to economize dense prose."},
            {"word": "participial", "partOfSpeech": "adjective", "definition": "Formed from or having the syntactic nature of a participle.", "cefrLevel": "C1", "collocations": ["participial phrase", "participial construction"], "exampleSentence": "A participial construction replaces lengthy subordinate causal clauses."},
            {"word": "dangling", "partOfSpeech": "adjective", "definition": "Lacking a clear grammatical modifier or agreement subject in the matrix clause.", "cefrLevel": "C1", "collocations": ["dangling participle", "dangling modifier"], "exampleSentence": "The editor corrected a dangling participle that distorted the author's meaning."}
        ]
    },

    # ==================== CEFR C2 ====================
    {
        "title": "Stylistic Inversion & Rhetorical Fronting",
        "level": "C2",
        "rule": """# Stylistic Inversion and Rhetorical Fronting at Mastery Level (CEFR C2)

## 1. Theoretical Scope & Stylistic Weight
At the C2 level, inversion ceases to be merely a grammatical exercise and becomes a sophisticated instrument for literary rhythm, cadence, topic continuity, and dramatic suspension.

## 2. Structural Typology

### A. Locative & Directional Inversion (Full Verb Inversion)
- Prepositional phrases of place or direction fronted before verbs of motion or posture:
  - *Down the staircase **rumbled the carriage**.*
  - *Across the valley **lay the ruins** of the medieval fortress.*
  - *Present at the ceremonial signing **were delegates** from twenty-six sovereign nations.*

### B. Comparative & Evaluative Fronting
- *So devastating **was the typhoon** that the entire province declared a state of emergency.*
- *Such **was the ferocity** of his rebuttal that the opposition conceded immediately.*

### C. Inverted Conditional Protasis (Omission of 'If')
- Present/Future: *Should you require further clarification, do not hesitate to contact our legal counsel.*
- Counterfactual Present: *Were it not for your generous intervention, the foundation would have collapsed.*
- Counterfactual Past: *Had the diplomatic envoy acted with greater discretion, the conflict might have been averted.*
""",
        "vocabulary": [
            {"word": "cadence", "partOfSpeech": "noun", "definition": "A rhythmic sequence or flow of sounds in language.", "cefrLevel": "C2", "collocations": ["rhetorical cadence", "measured cadence"], "exampleSentence": "Stylistic inversion imparts an elevated cadence to oratorical addresses."},
            {"word": "protasis", "partOfSpeech": "noun", "definition": "The subordinate clause expressing the condition in a conditional sentence.", "cefrLevel": "C2", "collocations": ["inverted protasis", "conditional protasis"], "exampleSentence": "Omission of 'if' in the protasis creates a dignified and formal register."},
            {"word": "oratorical", "partOfSpeech": "adjective", "definition": "Relating to the art or practice of formal speaking in public.", "cefrLevel": "C2", "collocations": ["oratorical eloquence", "oratorical style"], "exampleSentence": "Churchill's speeches were renowned for their oratorical gravity and structural inversion."}
        ]
    },
    {
        "title": "Idiomatic Phrasal Collocations & Register Shifts",
        "level": "C2",
        "rule": """# High-Register Collocational Networks and Nuanced Register Modulation (CEFR C2)

## 1. Register Architecture: Anglo-Saxon vs Greco-Latin Precision
Near-synonyms carry intense social, legal, and pragmatic connotations that must be controlled with native-like mastery:
- Colloquial / Anglo-Saxon: *look into, put up with, call off, leave out, turn down*.
- Elevated / Academic / Diplomatic: *investigate / probe, tolerate / brook, abrogate / revoke, omit / exclude, decline / spurn*.

## 2. Formulaic Collocations and Lexical Stance
Mastery requires deployment of delexicalized and prepositional collocations that express fine epistemic stance:
- *to brook no interference* (tolerate zero disruption).
- *to run the gauntlet of public scrutiny*.
- *in flagrant contravention of international treaties*.
- *to pay lip service to ethical obligations*.
""",
        "vocabulary": [
            {"word": "abrogate", "partOfSpeech": "verb", "definition": "To repeal or do away with a law, right, or formal agreement officially.", "cefrLevel": "C2", "collocations": ["abrogate a treaty", "abrogate responsibilities"], "exampleSentence": "The legislature moved to abrogate the obsolete bilateral agreement."},
            {"word": "contravention", "partOfSpeech": "noun", "definition": "An action which violates a law, rule, or treaty.", "cefrLevel": "C2", "collocations": ["in contravention of", "direct contravention"], "exampleSentence": "The military expansion was enacted in direct contravention of the armistice."},
            {"word": "epistemic", "partOfSpeech": "adjective", "definition": "Relating to knowledge or to the degree of certainty in a statement.", "cefrLevel": "C2", "collocations": ["epistemic stance", "epistemic modality"], "exampleSentence": "The ambassador modified her epistemic stance using subtle modal qualifications."}
        ]
    }
]

def main():
    print(f"Seeding comprehensive curriculum data to {CURRICULUM_DIR}...")
    
    written_rules = 0
    written_vocabs = 0

    for topic in TOPICS_DATA:
        title = topic["title"]
        slug = to_slug(title)

        # Write Rule Markdown
        rule_filename = f"{slug}.md"
        rule_path = os.path.join(RULES_DIR, rule_filename)
        with open(rule_path, "w", encoding="utf-8") as f:
            f.write(topic["rule"].strip() + "\n")
        written_rules += 1

        # Write Vocabulary JSON
        vocab_filename = f"{slug}.json"
        vocab_path = os.path.join(VOCAB_DIR, vocab_filename)
        vocab_words = [item["word"] for item in topic["vocabulary"]]
        with open(vocab_path, "w", encoding="utf-8") as f:
            json.dump(vocab_words, f, indent=2, ensure_ascii=False)
        written_vocabs += 1

        # Also write detailed vocabulary dictionary
        vocab_detailed_filename = f"{slug}_detailed.json"
        vocab_detailed_path = os.path.join(VOCAB_DIR, vocab_detailed_filename)
        with open(vocab_detailed_path, "w", encoding="utf-8") as f:
            json.dump(topic["vocabulary"], f, indent=2, ensure_ascii=False)

    print(f"Successfully generated {written_rules} comprehensive rule files and {written_vocabs} vocabulary datasets.")

if __name__ == "__main__":
    main()
