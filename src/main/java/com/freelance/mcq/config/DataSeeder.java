package com.freelance.mcq.config;


import com.freelance.mcq.entity.MockTest;
import com.freelance.mcq.entity.Question;
import com.freelance.mcq.entity.Subject;
import com.freelance.mcq.repository.MockTestRepository;
import com.freelance.mcq.repository.QuestionRepository;
import com.freelance.mcq.repository.SubjectRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final SubjectRepository subjectRepository;
    private final MockTestRepository mockTestRepository;
    private final QuestionRepository questionRepository;

    public DataSeeder(SubjectRepository subjectRepository, MockTestRepository mockTestRepository,
                       QuestionRepository questionRepository) {
        this.subjectRepository = subjectRepository;
        this.mockTestRepository = mockTestRepository;
        this.questionRepository = questionRepository;
    }

    @Override
    public void run(String... args) {
        if (subjectRepository.count() > 0) {
            System.out.println("Data already seeded, skipping.");
            return;
        }

        System.out.println("Seeding database...");

        // ===== HISTORY =====
        Subject history = subjectRepository.save(new Subject("history", "History", "time-outline"));

        MockTest hist1 = mockTestRepository.save(new MockTest(history, "hist_1", "Ancient Indian History", 5, 15, false));
        questionRepository.saveAll(List.of(
            new Question(hist1, "Who was the founder of the Maurya Empire?",
                List.of("Ashoka", "Chandragupta Maurya", "Bindusara", "Harsha"), 1,
                "Chandragupta Maurya established the empire in 322 BCE with the help of Chanakya.",
                "easy", "Ancient History"),
            new Question(hist1, "Which edict mentions the Kalinga War victory of Ashoka?",
                List.of("Rock Edict XIII", "Rock Edict V", "Pillar Edict VII", "Minor Rock Edict I"), 0,
                "Major Rock Edict XIII describes the Kalinga War and Ashoka's shift toward Buddhism.",
                "hard", "Mauryan Administration"),
            new Question(hist1, "Which Gupta ruler is associated with the 'Golden Age' of India?",
                List.of("Samudragupta", "Chandragupta II", "Kumaragupta", "Skandagupta"), 1,
                "Chandragupta II (Vikramaditya) presided over a peak in art, science, and literature.",
                "medium", "Gupta Empire"),
            new Question(hist1, "The Indus Valley site of Mohenjo-daro is located in present-day:",
                List.of("India", "Pakistan", "Afghanistan", "Nepal"), 1,
                "Mohenjo-daro lies in Sindh, Pakistan, along the Indus River.",
                "easy", "Indus Valley Civilization"),
            new Question(hist1, "How many Vedas are there in ancient Indian scripture?",
                List.of("3", "4", "5", "6"), 1,
                "The four Vedas are Rigveda, Yajurveda, Samaveda, and Atharvaveda.",
                "easy", "Vedic Period")
        ));

        MockTest hist2 = mockTestRepository.save(new MockTest(history, "hist_2", "Medieval History & Empire", 5, 15, false));
        questionRepository.saveAll(List.of(
            new Question(hist2, "Who founded the Delhi Sultanate?",
                List.of("Qutb-ud-din Aibak", "Muhammad Ghori", "Iltutmish", "Balban"), 0,
                "Qutb-ud-din Aibak established the Delhi Sultanate in 1206 as its first ruler.",
                "medium", "Delhi Sultanate"),
            new Question(hist2, "Who was the first Mughal emperor of India?",
                List.of("Akbar", "Humayun", "Babur", "Shah Jahan"), 2,
                "Babur founded the Mughal Empire after winning the First Battle of Panipat in 1526.",
                "easy", "Mughal Empire"),
            new Question(hist2, "The Taj Mahal was built by which Mughal emperor?",
                List.of("Akbar", "Jahangir", "Shah Jahan", "Aurangzeb"), 2,
                "Shah Jahan built the Taj Mahal in memory of his wife Mumtaz Mahal.",
                "easy", "Mughal Empire"),
            new Question(hist2, "Which policy allowed the British East India Company to annex princely states with no natural heir?",
                List.of("Subsidiary Alliance", "Doctrine of Lapse", "Permanent Settlement", "Ryotwari System"), 1,
                "The Doctrine of Lapse was introduced by Lord Dalhousie.",
                "hard", "Colonial Policies"),
            new Question(hist2, "Who administered the religious policy of 'Din-i-Ilahi'?",
                List.of("Babur", "Akbar", "Jahangir", "Aurangzeb"), 1,
                "Akbar introduced Din-i-Ilahi as a syncretic religious philosophy in 1582.",
                "medium", "Mughal Empire")
        ));

        MockTest hist3 = mockTestRepository.save(new MockTest(history, "hist_3", "Modern Freedom Movement", 5, 15, false));
        questionRepository.saveAll(List.of(
            new Question(hist3, "Who founded the Indian National Congress in 1885?",
                List.of("Mahatma Gandhi", "A.O. Hume", "Bal Gangadhar Tilak", "Dadabhai Naoroji"), 1,
                "Allan Octavian Hume, a retired British civil servant, founded the INC in 1885.",
                "medium", "Freedom Movement"),
            new Question(hist3, "The Quit India Movement was launched in which year?",
                List.of("1930", "1942", "1920", "1947"), 1,
                "Gandhi launched the Quit India Movement on 8 August 1942.",
                "easy", "Freedom Movement"),
            new Question(hist3, "Who gave the famous slogan 'Swaraj is my birthright'?",
                List.of("Bal Gangadhar Tilak", "Lala Lajpat Rai", "Bipin Chandra Pal", "Subhas Chandra Bose"), 0,
                "Bal Gangadhar Tilak coined this slogan during the freedom struggle.",
                "medium", "Freedom Movement"),
            new Question(hist3, "The Jallianwala Bagh massacre took place in which city?",
                List.of("Lahore", "Amritsar", "Delhi", "Ludhiana"), 1,
                "The massacre occurred in Amritsar, Punjab, on 13 April 1919.",
                "easy", "Freedom Movement"),
            new Question(hist3, "India gained independence on which date?",
                List.of("15 August 1947", "26 January 1950", "2 October 1947", "15 August 1950"), 0,
                "India became independent on 15 August 1947; the Republic was established 26 January 1950.",
                "easy", "Freedom Movement")
        ));

        // ===== GEOGRAPHY =====
        Subject geography = subjectRepository.save(new Subject("geography", "Geography", "earth-outline"));

        MockTest geo1 = mockTestRepository.save(new MockTest(geography, "geo_1", "Physical Geography Basics", 5, 15, false));
        questionRepository.saveAll(List.of(
            new Question(geo1, "Which is the longest river in the world?",
                List.of("Amazon", "Nile", "Yangtze", "Ganga"), 1,
                "The Nile is generally regarded as the world's longest river at ~6,650 km.",
                "easy", "Physical Geography"),
            new Question(geo1, "Mount Everest is located in which mountain range?",
                List.of("Andes", "Alps", "Himalayas", "Rockies"), 2,
                "Mount Everest lies in the Himalayas on the border of Nepal and Tibet.",
                "easy", "Physical Geography"),
            new Question(geo1, "Which layer of the Earth is primarily composed of molten rock?",
                List.of("Crust", "Mantle", "Outer Core", "Inner Core"), 1,
                "The mantle is largely composed of semi-molten rock called magma.",
                "medium", "Physical Geography"),
            new Question(geo1, "Which river basin is the largest in the world by discharge volume?",
                List.of("Nile Basin", "Amazon Basin", "Ganga Basin", "Mississippi Basin"), 1,
                "The Amazon Basin discharges more water than the next several largest rivers combined.",
                "hard", "River Systems"),
            new Question(geo1, "Which type of rock is formed from cooled magma or lava?",
                List.of("Sedimentary", "Metamorphic", "Igneous", "Organic"), 2,
                "Igneous rocks form from the solidification of molten magma or lava.",
                "medium", "Rocks & Minerals")
        ));

        MockTest geo2 = mockTestRepository.save(new MockTest(geography, "geo_2", "Indian Climate & Vegetation", 5, 15, false));
        questionRepository.saveAll(List.of(
            new Question(geo2, "Which wind system brings the majority of India's annual rainfall?",
                List.of("Trade Winds", "Southwest Monsoon", "Westerlies", "Polar Easterlies"), 1,
                "The Southwest Monsoon (June-September) delivers most of India's rainfall.",
                "easy", "Indian Climate"),
            new Question(geo2, "Which type of vegetation is dominant in the Thar Desert region?",
                List.of("Tropical Evergreen", "Xerophytic (thorny) scrub", "Mangrove", "Alpine"), 1,
                "The Thar Desert supports drought-resistant xerophytic and thorny scrub vegetation.",
                "medium", "Indian Vegetation"),
            new Question(geo2, "The Western Ghats primarily support which type of forest?",
                List.of("Tropical Evergreen", "Tropical Deciduous", "Mangrove", "Coniferous"), 0,
                "High rainfall along the Western Ghats sustains tropical evergreen forests.",
                "medium", "Indian Vegetation"),
            new Question(geo2, "Which climate classification does most of peninsular India fall under (Koppen)?",
                List.of("Tropical Savanna (Aw)", "Humid Continental", "Mediterranean", "Tundra"), 0,
                "Much of peninsular India falls under the Tropical Savanna (Aw) climate type.",
                "hard", "Indian Climate"),
            new Question(geo2, "Mangrove forests in India are most extensively found in which region?",
                List.of("Rajasthan", "Sundarbans (West Bengal)", "Ladakh", "Punjab"), 1,
                "The Sundarbans host the largest mangrove forest in India and the world.",
                "easy", "Indian Vegetation")
        ));

        // ===== POLITY =====
        Subject polity = subjectRepository.save(new Subject("polity", "Political Science", "book-outline"));

        MockTest pol1 = mockTestRepository.save(new MockTest(polity, "pol_1", "Indian Constitution Basics", 5, 15, false));
        questionRepository.saveAll(List.of(
            new Question(pol1, "When did the Indian Constitution come into effect?",
                List.of("15 Aug 1947", "26 Jan 1950", "26 Nov 1949", "2 Oct 1950"), 1,
                "The Constitution was adopted on 26 Nov 1949 and came into effect on 26 Jan 1950.",
                "easy", "Indian Polity"),
            new Question(pol1, "Who is known as the 'Father of the Indian Constitution'?",
                List.of("Jawaharlal Nehru", "B.R. Ambedkar", "Rajendra Prasad", "Sardar Patel"), 1,
                "Dr. B.R. Ambedkar chaired the Drafting Committee of the Constitution.",
                "easy", "Indian Polity"),
            new Question(pol1, "From which country did India borrow the concept of the Directive Principles of State Policy?",
                List.of("USA", "UK", "Ireland", "Canada"), 2,
                "The Directive Principles were borrowed from the Irish Constitution.",
                "hard", "Indian Polity"),
            new Question(pol1, "Which article of the Constitution abolishes untouchability?",
                List.of("Article 14", "Article 17", "Article 21", "Article 32"), 1,
                "Article 17 abolishes 'untouchability' and forbids its practice in any form.",
                "medium", "Fundamental Rights"),
            new Question(pol1, "How many schedules did the original Indian Constitution have?",
                List.of("8", "12", "10", "9"), 0,
                "The original Constitution had 8 schedules; it now has 12 after amendments.",
                "hard", "Indian Polity")
        ));

        // pol_2 marked premium=true as an example — flip if you want it free too
        MockTest pol2 = mockTestRepository.save(new MockTest(polity, "pol_2", "Preamble & Fundamental Rights", 5, 15, true));
        questionRepository.saveAll(List.of(
            new Question(pol2, "The Preamble declares India to be a sovereign, socialist, secular, and:",
                List.of("Federal Republic", "Democratic Republic", "Parliamentary Republic", "Unitary Republic"), 1,
                "The Preamble describes India as a Sovereign Socialist Secular Democratic Republic.",
                "easy", "Preamble"),
            new Question(pol2, "The words 'Socialist' and 'Secular' were added to the Preamble by which amendment?",
                List.of("42nd Amendment", "44th Amendment", "24th Amendment", "1st Amendment"), 0,
                "The 42nd Amendment Act (1976) inserted 'Socialist' and 'Secular' into the Preamble.",
                "hard", "Preamble"),
            new Question(pol2, "Which Fundamental Right abolishes titles (except military/academic)?",
                List.of("Right to Equality", "Right to Freedom", "Right against Exploitation", "Cultural & Educational Rights"), 0,
                "Article 18, under Right to Equality, abolishes titles conferred by the State.",
                "medium", "Fundamental Rights"),
            new Question(pol2, "Which article guarantees the Right to Constitutional Remedies?",
                List.of("Article 19", "Article 21", "Article 32", "Article 25"), 2,
                "Article 32, empowering citizens to move the Supreme Court for enforcement of rights, is called the 'Heart and Soul' of the Constitution.",
                "medium", "Fundamental Rights"),
            new Question(pol2, "Fundamental Rights are enshrined in which Part of the Constitution?",
                List.of("Part II", "Part III", "Part IV", "Part V"), 1,
                "Fundamental Rights are laid out in Part III (Articles 12-35) of the Constitution.",
                "easy", "Fundamental Rights")
        ));

        System.out.println("Seeding complete.");
    }
}
