import type { ICareergoCoverLetter } from "../../models/careergo/ICareergoCoverLetter.ts";
import type { ICareergoResume, CareergoResumeSection } from "../../models/careergo/ICareergoResume.ts";
import { CAREERGO_RESUME_DEFAULT_SECTION_ORDER } from "../../models/careergo/ICareergoResume.ts";
import {
    CLASSIC_RESUME_CSS,
    RESUME_PAGINATION_CSS,
    RESUME_SEMANTIC_UTILITY_CSS,
    RESUME_STRUCTURAL_PRINT_CSS,
    TEMPLATE_VARS_LAYOUT1
} from "./recovered-template-css.ts";

const escapeHtml = (value: string) =>
    value.replace(
        /[&<>"']/g,
        (character) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[character]!
    );

const lines = (value: string) => escapeHtml(value).replace(/\n/g, "<br>");

function section(title: string, content: string): string {
    if (!content) return "";
    return `<section class="page-section classic-page-section"><div class="classic-card"><div class="classic-section-header"><div class="classic-section-header-inner"><strong class="classic-font classic-section-title-text">${escapeHtml(title)}</strong></div></div>${content}</div></section>`;
}

function entry(title: string, subtitle: string, detail: string, bullets: string[] = []): string {
    return `<div class="classic-entry">${title || subtitle ? `<div class="classic-row-between"><strong>${escapeHtml(title)}</strong><span class="classic-font-italic">${escapeHtml(subtitle)}</span></div>` : ""}${detail ? `<div class="classic-font-italic">${escapeHtml(detail)}</div>` : ""}${bullets.length ? `<ul>${bullets.map((bullet) => `<li><span>${lines(bullet)}</span></li>`).join("")}</ul>` : ""}</div>`;
}

/**
 * Joins a start and end date with an en dash.
 * @param start Start date text.
 * @param end End date text or "Present".
 * @returns A date range such as "2019 – Present", or an empty string.
 */
function dateSpan(start: string | undefined, end: string | undefined): string {
    return [start, end].filter(Boolean).join(" – ");
}

/**
 * Builds recipient and greeting lines like Careergo's cover-letter template.
 */
export function classicCoverLetterAddressing(letter: ICareergoCoverLetter): {
    recipientLines: string[];
    greeting: string | null;
} {
    const recipientLines = [letter.hiringManagerName, letter.companyName].filter((value): value is string =>
        Boolean(value?.trim())
    );
    const leadingText = letter.letterContent
        .replace(/<!--[\s\S]*?-->/g, "")
        .replace(/<br\s*\/?\s*>/gi, " ")
        .replace(/<[^>]+>/g, " ")
        .replace(/&nbsp;|&#160;/gi, " ")
        .replace(/&amp;/gi, "&")
        .replace(/\s+/g, " ")
        .trimStart();
    const alreadyHasGreeting = /^(?:dear\b|hello\b|to\s+whom\s+it\s+may\s+concern\b)/i.test(leadingText);
    return {
        recipientLines,
        greeting: alreadyHasGreeting ? null : `Dear ${letter.hiringManagerName?.trim() || "Hiring Manager"},`
    };
}

function resumeSections(resume: ICareergoResume): Record<CareergoResumeSection, string> {
    return {
        professionalSummary: section(
            "Professional Summary",
            resume.professionalSummary
                ? `<div class="classic-summary-content">${lines(resume.professionalSummary)}</div>`
                : ""
        ),
        workExperience: section(
            "Work Experience",
            resume.employments
                .map((item) =>
                    entry(
                        item.jobTitle,
                        item.employer,
                        [dateSpan(item.startDate, item.isPresent ? "Present" : item.endDate), item.city, item.country]
                            .filter(Boolean)
                            .join(" · "),
                        item.achievements
                    )
                )
                .join("")
        ),
        education: section(
            "Education",
            resume.education
                .map((item) =>
                    entry(
                        item.degree,
                        item.institution,
                        [dateSpan(item.startDate, item.isPresent ? "Present" : item.endDate), item.city, item.country]
                            .filter(Boolean)
                            .join(" · ")
                    )
                )
                .join("")
        ),
        projects: section(
            "Projects",
            resume.projects.map((item) => entry(item.name, item.technologies.join(", "), item.description)).join("")
        ),
        certifications: section(
            "Certifications",
            resume.certifications
                .map((item) =>
                    entry(item.name, item.issuer, [item.issueDate, item.credentialId].filter(Boolean).join(" · "))
                )
                .join("")
        ),
        accomplishments: section(
            "Accomplishments",
            resume.accomplishments.map((item) => entry(item.title, item.date, item.description)).join("")
        ),
        professionalDevelopment: section(
            "Professional Development",
            resume.professionalDevelopment
                .map((item) =>
                    entry(
                        item.course_name,
                        item.institution,
                        [item.issued_date, item.description].filter(Boolean).join(" · ")
                    )
                )
                .join("")
        ),
        professionalMemberships: section(
            "Professional Memberships",
            resume.professionalMemberships
                .map((item) =>
                    entry(
                        item.organization_name,
                        item.membership_type,
                        [item.member_since, item.notes].filter(Boolean).join(" · ")
                    )
                )
                .join("")
        ),
        volunteerExperience: section(
            "Volunteer Experience",
            resume.volunteerExperience
                .map((item) =>
                    entry(
                        item.role,
                        item.organization,
                        [item.start_date, item.end_date, item.description].filter(Boolean).join(" · ")
                    )
                )
                .join("")
        ),
        languages: section(
            "Languages",
            resume.languages.map((item) => entry(item.language_name, item.proficiency, item.certification)).join("")
        ),
        skills: section(
            "Skills",
            resume.skills ? `<div class="classic-summary-content">${lines(resume.skills)}</div>` : ""
        ),
        referees: section(
            "Referees",
            resume.referees
                .map((item) =>
                    entry(item.name, item.position, [item.company, item.email, item.phone].filter(Boolean).join(" · "))
                )
                .join("")
        )
    };
}

/**
 * Creates a printable HTML document using Careergo's Classic resume styles.
 */
export function buildClassicResumeHtml(resume: ICareergoResume): string {
    const details = resume.personalDetails;
    const name = [details.firstName, details.lastName].filter(Boolean).join(" ");
    const contact = [
        details.address,
        details.city,
        details.country,
        details.phoneNumber,
        details.email,
        details.linkedin,
        details.website
    ]
        .filter(Boolean)
        .join(" · ");
    const sections = resumeSections(resume);
    const order = [
        ...new Set([
            ...(resume.sectionOrder ?? CAREERGO_RESUME_DEFAULT_SECTION_ORDER),
            ...CAREERGO_RESUME_DEFAULT_SECTION_ORDER
        ])
    ];
    const content = order.map((key) => sections[key]).join("");
    return documentHtml(
        `<div class="resume-template page"><div class="body"><div class="builder-content classic-builder-content"><div class="classic-header-block classic-card"><header class="header" data-header-align="center" style="--border-width-header-bottom:0;--spacing-header-bottom:0;--spacing-header-content:1px;--font-size-full-name:24px;--font-size-contact-details:14px"><div class="header-content classic-header-stack"><div class="classic-header-name-row"><div class="classic-header-name-inner"><h1 class="full-name classic-header-name-field">${escapeHtml(name || "Curriculum Vitae")}</h1></div></div><div class="classic-header-contact-row"><div class="classic-header-contact-inner"><div class="contact-details">${escapeHtml(contact)}</div></div></div></div></header></div>${content}</div></div></div>`,
        `${TEMPLATE_VARS_LAYOUT1}${RESUME_STRUCTURAL_PRINT_CSS}${RESUME_SEMANTIC_UTILITY_CSS}${CLASSIC_RESUME_CSS}${RESUME_PAGINATION_CSS}`
    );
}

/**
 * Creates a printable HTML document using Careergo's cover letter layout.
 */
export function buildClassicCoverLetterHtml(letter: ICareergoCoverLetter): string {
    const addressing = classicCoverLetterAddressing(letter);
    const sender = [letter.address, letter.email, letter.phoneNumber, letter.linkedin]
        .filter((item): item is string => Boolean(item))
        .map((item) => `<p>${escapeHtml(item)}</p>`)
        .join("");
    const recipient = addressing.recipientLines.map((item) => `<p>${escapeHtml(item)}</p>`).join("");
    const body = letter.letterContent
        .split(/\n\s*\n/)
        .map((paragraph) => `<p>${lines(paragraph.trim())}</p>`)
        .join("");
    const content = `<main class="cover-letter-page"><div class="cover-letter-correspondence"><div class="cover-letter-recipient">${recipient}</div><div class="cover-letter-sender"><p class="cover-letter-sender-name">${escapeHtml(letter.fullName)}</p>${sender}</div></div>${addressing.greeting ? `<p class="cover-letter-greeting">${escapeHtml(addressing.greeting)}</p>` : ""}<div class="cover-letter-body">${body}</div></main>`;
    const css = `html,body{margin:0;padding:0;background:#fff;color:#000;font-family:'Times New Roman',Times,serif;font-size:12pt;line-height:1.5}.cover-letter-page{box-sizing:border-box;width:100%;padding:0;background:#fff;overflow-wrap:anywhere;word-break:break-word}.cover-letter-correspondence{display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1fr);gap:3rem;margin-bottom:2rem}.cover-letter-recipient,.cover-letter-sender{min-width:0}.cover-letter-recipient p,.cover-letter-sender p{margin:0}.cover-letter-sender{text-align:right}.cover-letter-sender-name{font-weight:700}.cover-letter-greeting{margin:0 0 .75rem}.cover-letter-body p{margin:.75rem 0}.cover-letter-body p:empty{display:none}@page{size:letter;margin:.5in .6in}`;
    return `<!doctype html><html lang="en"><head><meta charset="utf-8"><title>Cover Letter</title><style>${css}</style></head><body>${content}</body></html>`;
}

function documentHtml(body: string, css: string): string {
    return `<!doctype html><html lang="en"><head><meta charset="utf-8"><title>CV</title><style>${css}@page{size:letter;margin:.75in}html,body{margin:0;padding:0;background:#fff}.resume-template{width:100%;max-width:100%;padding:0!important}.body{width:100%}</style></head><body>${body}</body></html>`;
}
