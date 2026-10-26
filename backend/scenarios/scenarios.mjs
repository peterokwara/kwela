/**
 * Real-life conversations the writer must handle well. Each turn is what the person types, in order.
 * The first turn of most scenarios is the tapped answer to "What's going on right now?".
 */
export const scenarios = [
    {
        name: "Cook on a shift, then interrupted",
        turns: [
            "I saw a job I want",
            "Cook at Serena Hotel Nairobi. Needs 2 yrs kitchen exp and food safety cert. Pls do my cv. Wanjiku Mwangi, 0712 345 678, wanjiku@example.test. Commis chef at Java House 2022 to now. Kitchen assistant at Mama Rocks 2020 to 2022. Food hygiene cert 2021. KCSE 2019.",
            "back sorry customers came, where were we"
        ]
    },
    {
        name: "Doctor with 90-second windows",
        turns: [
            "My job is ending",
            "Dr Otieno. Medical officer at Kisumu County Referral, contract ends Dec. MBChB UoN 2016, internship Kenyatta National 2017, KMPDC licensed. Applying: Medical Officer at Aga Khan Nairobi, needs ACLS and 3 yrs experience. Please do CV and letter. 0701 222 333, otieno@example.test",
            "ok",
            "fine"
        ]
    },
    {
        name: "Bartender in a club, no CV",
        turns: [
            "Just starting out",
            "i work as a bartender at a club in westlands 3 yrs, before that waiter 2 yrs, no cv. want bar supervisor jobs",
            "Brian Kimani 0722 111 222"
        ]
    },
    {
        name: "No CV at all, just finished school",
        turns: ["Something else", "i dont have a cv i just finished form 4 and i do mama fua, want a job in a supermarket"]
    },
    {
        name: "Swahili",
        turns: [
            "Nataka kazi ya udereva wa matatu au lori",
            "Naitwa Hassan Juma. Nina leseni ya PSV, nimeendesha lori Mombasa miaka 4. CV sina. Simu 0733 000 111. Nataka kazi ya udereva wa lori Nairobi."
        ]
    },
    {
        name: "The correction: I still work there",
        turns: [
            "I saw a job I want",
            "Peter Okwara, Nairobi, peter@example.test, 0700 000 000. Senior Full Stack Engineer. Careergo Jan 2023 to present: built CV tools in SvelteKit and Cloudflare, led a team of 3. Techno Brain 2019 to 2022: web apps in Node and React. BSc Computer Science 2018. Skills TypeScript, Svelte, React, Node, SQL. Job: Senior Full Stack Engineer at Techno Brain Nairobi, needs TypeScript, React, Node, cloud, mentoring. Please prepare my CV, cover letter and answers.",
            "careergo i still work there bruv"
        ]
    },
    {
        name: "Answering the writer's quick question, then changing a line",
        turns: [
            "I want better pay",
            "Moses Kamau, Nakuru, moses@example.test, 0722 800 900. Store keeper at Nakuru Wholesalers 2019 to present: responsible for receiving and issuing stock, kept daily stock records for the warehouse, handled supplier deliveries. Diploma in Supply Chain, Rift Valley Technical 2018. Applying: Stores Supervisor at Kenchic Nairobi. Please prepare my CV and cover letter.",
            "@review:Dozens",
            "@change:make it sound less boastful"
        ]
    },
    {
        name: "Long job page pasted with its link",
        turns: [
            "I saw a job I want",
            "Senior Full Stack Engineer\nTriggerise, Nairobi, Kenya (Hybrid)\nAbout Triggerise\nTriggerise builds technology that helps brands and communities reach the people they serve across Africa. We work with partners in health, finance, and consumer goods, and our platform connects millions of people to rewards and services every month.\nThe role\nWe are looking for a Senior Full Stack Engineer to join our product team. You will design, build and ship features across our web and mobile platforms, work closely with product managers and designers, and mentor other engineers.\nResponsibilities\n- Design and build scalable web applications using TypeScript, React and Node.js\n- Own features end to end, from discussion with product to production monitoring\n- Write clean, tested, maintainable code and review the code of others\n- Improve the performance, reliability and security of our systems\n- Work with cloud infrastructure on AWS and Cloudflare\n- Mentor junior and mid-level engineers\nRequirements\n- 5+ years of professional software engineering experience\n- Strong skills in TypeScript, React, Node.js and SQL\n- Experience with cloud platforms and CI/CD\n- Ability to communicate clearly with technical and non-technical teams\n- Experience mentoring other engineers\nNice to have\n- Experience with Svelte, PostgreSQL and event-driven architectures\n- Experience in fintech or mobile money\nWhat we offer\nCompetitive salary, health insurance, hybrid work, learning budget and the chance to build products used by millions.\nApply for this position\nFull name, email, phone, LinkedIn, upload your CV and cover letter.\nPrivacy notice\nTriggerise is committed to respecting and protecting your personal data during the recruitment process. Click here to view the document. By applying for this position, you acknowledge that you have read and understood our privacy notice.\nPrivacy Policy\nTerms of Service\n(c) BambooHR All rights reserved.\n\nthis job https://triggerise.bamboohr.com/careers/707?source=aWQ9MzM%3D\n\nand my cv is this one\n\nPeter Okwara, Nairobi, Kenya. peter@example.test, 0700 000 000. Senior Full Stack Engineer. Careergo, Jan 2023 to present: built CV and application tools in SvelteKit, TypeScript and Cloudflare, led a team of 3, delivered an AI product with 12,000 paying customers. Techno Brain 2019 to 2022: built web apps in Node and React, mentored two junior developers. BSc Computer Science, 2018. Skills: TypeScript, Svelte, React, Node, SQL, Cloudflare, PostgreSQL.",
        ]
    },
    {
        name: "Adds certifications to a finished CV",
        turns: [
            "I saw a job I want",
            "Peter Okwara, Nairobi, peter@example.test, 0700 000 000. Senior Full Stack Engineer. Careergo Jan 2023 to present: built CV tools in SvelteKit and Cloudflare, led a team of 3. Techno Brain 2019 to 2022: web apps in Node and React. BSc Computer Science 2018. Skills TypeScript, Svelte, React, Node, SQL. Job: Senior Full Stack Engineer at Techno Brain Nairobi, needs TypeScript, React, Node, cloud, mentoring. Please prepare my CV and cover letter.",
            "please add my certifications: AWS Certified Cloud Practitioner from Amazon in 2024, and Google Associate Cloud Engineer in 2023"
        ]
    },
    {
        name: "Researches pay online",
        turns: [
            "I want better pay",
            "Peter Okwara, Nairobi, peter@example.test, 0700 000 000. CTO at Careergo since Jan 2023, led a team of 3, built SaaS products in TypeScript and Cloudflare. Applying for Deputy CTO at Tiko in Nairobi, the listing shows no salary. Please prepare my CV. Then please search online and see what pay range i should have for that role"
        ]
    },
    {
        name: "Pay question on its own",
        turns: ["I want better pay", "What pay range should I ask for as a Deputy CTO at a tech startup in Nairobi? Please search online."]
    },
    {
        name: "Any luck follow-up",
        turns: [
            "I saw a job I want",
            "Peter Okwara, Nairobi, peter@example.test, 0700 000 000. CTO at Careergo since Jan 2023, led a team of 3, built SaaS products in TypeScript and Cloudflare. BSc Computer Science 2018. Applying for Deputy CTO at Tiko in Nairobi. Please prepare my CV and cover letter.",
            "also change my title to Co-founder and CTO",
            "any luck?"
        ]
    },
    {
        name: "Teacher with a long CV",
        turns: [
            "I want better pay",
            "Grace Achieng, Kisumu, grace@example.test, 0711 555 444. Teacher of English and Literature. St Mary's Girls Secondary 2015 to present: head of department since 2019, KCSE English mean raised from 5.8 to 7.1, coach of drama club that reached national festival 2021 and 2023, mentor to 4 new teachers, set and marked mock exams, organised parents' days. Previously Nyanza Academy 2011 to 2015: taught forms 1 to 4, class teacher form 2. B.Ed Arts Maseno University 2010. TSC registered. Skills: curriculum planning, classroom management, Google Workspace. Referees available. Applying: Head of Languages at Alliance Girls High School, Kikuyu, needs 8 years and leadership. Do CV and letter.",
            "make it shorter"
        ]
    },
    {
        name: "Sheng",
        turns: ["niaje bro nataka job ya mauzo, mi ni msee wa field", "jina ni Kevo Otieno, nimekuwa sales rep kwa Bidco 2 yrs, ntaka job ya sales exec Nairobi. namba 0745 666 777"]
    },
    {
        name: "Job ad full of noise",
        turns: [
            "I saw a job I want",
            "🔥🔥 WE ARE HIRING!!! 🔥🔥 Like and share to help others!! Bakery Factory Supervisor, Nairobi. Min 2 yrs exp, manage 40 staff, KES 60,000. Apply now now now!! Join our Telegram for more jobs!! Terms apply. ---- My details: Mary Njeri, 0799 123 456, mary@example.test. Baker at Brookside Bakery 2020 to now, shift lead for 10 staff since 2022. Certificate in food production, Kenya Utalii 2019. Please prepare my CV and cover letter."
        ]
    }
];
