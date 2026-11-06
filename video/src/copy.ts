/**
 * Every line of text in the short cut, in one place. Edit here; the video picks it up.
 * Captions are the big Sheng text. The rest is what appears on the phone screens.
 */
export const copy = {
  captions: {
    hook: "Manze, cyber wanakucharge 2K ya CV moja?",
    share: ["Job imepostiwa kwa WhatsApp group?", "Wacha Kwela achangamkie, brathe."], // second line fades in later
    chat: ["Kwela anaona hiyo wera.", "Kwela anaulizia details za hiyo job."], // second line fades in later
    cards: "CV, cover letter na answers za hiyo job ziko bie",
    end: "Wewe unasubmit tu brathe. Link kwa bio.",
  },

  // Scene 2: a WhatsApp group (drawn from memory, not a real recording)
  whatsapp: {
    groupName: "Job Vacancies KE",
    groupMembers: "Brian, Faith, Kevin, You",
    dateChip: "TODAY",
    messages: [
      { from: "~ Faith W", text: "Anyone has seen the Naivas one?", time: "9:38 am" },
      { from: "~ Brian K", image: true, text: "Wasee hii ni ya Nairobi. Deadline 13th.", time: "9:41 am" },
    ],
    reply: { text: "Thanks bro", time: "9:44 am" },
    menu: ["Info", "Copy", "Share", "Report"],
    inputPlaceholder: "Message",
  },

  // The Android share sheet
  shareSheet: {
    title: "Sharing 1 image",
    fileName: "IMG-20261028-WA0012.jpg",
    targets: ["Quick Share", "Messages", "Kwela", "Drive"],
  },

  // Scenes 3 and 4: the Kwela app. The app itself is in English.
  kwela: {
    name: "Kwela",
    inputPlaceholder: "Message Kwela",
    timeHeader: "Today 10:24 AM",
    chat: {
      you: "Hebu cheki file yangum maze.", // what the app sends when you attach a file with no text
      attachmentLabel: "IMG-20261028-WA0012.jpg",
      replies: [
        "Nimeiiiget. tebu nipatie dakika utatu niweke hii stuff pamoja.",
        "Tebu nishow? Uko na experience ya pharmacy ya how many years??",
      ],
      chips: ["Under a year", "1-2 years", "3+ years"],
    },
    cards: {
      intro: "Application yako ya  Pharmaceutical Technologist iko ready brathe.",
      files: [
        { title: "CV - Pharmaceutical Technologist", job: "Pharmaceutical Technologist", preview: "Diploma in Pharmaceutical Technology. Enrolled with the Pharmacy and Poisons Board. Two years in a busy retail pharmacy." },
        { title: "Cover letter - Pharmaceutical Technologist", job: "Pharmaceutical Technologist", preview: "Dear Hiring Team, I am applying for the Pharmaceutical Technologist role in Nairobi. I have two years in a busy pharmacy." },
        { title: "Application answers - Pharmaceutical Technologist", job: "Pharmaceutical Technologist", preview: "Why do you want this role? I want to grow in a pharmacy team that serves customers every day." },
      ],
      buttons: ["Open", "Download", "Share"], // fixed by the app
    },
  },
} as const;
