import { z } from "zod";

export interface ICareergoPersonalDetails {
    /**
     * Candidate's first name.
     */
    firstName: string;
    /**
     * Candidate's last name.
     */
    lastName: string;
    /**
     * Candidate's email address.
     */
    email: string;
    /**
     * Candidate's phone number.
     */
    phoneNumber: string;
    /**
     * Candidate's street or postal address.
     */
    address: string;
    /**
     * Candidate's city.
     */
    city: string;
    /**
     * Candidate's state, county, or region.
     */
    state: string;
    /**
     * Candidate's country.
     */
    country: string;
    /**
     * Candidate's postal code.
     */
    postalCode: string;
    /**
     * Candidate's LinkedIn profile URL.
     */
    linkedin: string;
    /**
     * Candidate's personal website URL.
     */
    website: string;
}

export interface ICareergoEmployment {
    /**
     * Role held by the candidate.
     */
    jobTitle: string;
    /**
     * Organization where the candidate worked.
     */
    employer: string;
    /**
     * Employment start date.
     */
    startDate: string;
    /**
     * Employment end date.
     */
    endDate: string;
    /**
     * Whether the candidate still works in this role.
     */
    isPresent: boolean;
    /**
     * Results and responsibilities from this role.
     */
    achievements: string[];
    /**
     * City where the role was based.
     */
    city: string;
    /**
     * State, county, or region where the role was based.
     */
    state: string;
    /**
     * Country where the role was based.
     */
    country: string;
}

export interface ICareergoEducation {
    /**
     * Degree, certificate, or qualification earned.
     */
    degree: string;
    /**
     * School or institution attended.
     */
    institution: string;
    /**
     * Study start date.
     */
    startDate: string;
    /**
     * Study end date.
     */
    endDate: string;
    /**
     * Whether the candidate is still studying.
     */
    isPresent: boolean;
    /**
     * City where the institution is based.
     */
    city: string;
    /**
     * State, county, or region where the institution is based.
     */
    state: string;
    /**
     * Country where the institution is based.
     */
    country: string;
}

export interface ICareergoProject {
    /**
     * Project name.
     */
    name: string;
    /**
     * What the project does or demonstrates.
     */
    description: string;
    /**
     * Project website or source URL.
     */
    url: string;
    /**
     * Project start date.
     */
    startDate: string;
    /**
     * Project end date.
     */
    endDate: string;
    /**
     * Languages, tools, or platforms used.
     */
    technologies: string[];
}

export interface ICareergoCertification {
    /**
     * Certification name.
     */
    name: string;
    /**
     * Organization that issued the certification.
     */
    issuer: string;
    /**
     * Date the certification was issued.
     */
    issueDate: string;
    /**
     * Date the certification expires, if applicable.
     */
    expiryDate: string;
    /**
     * Credential identifier.
     */
    credentialId: string;
    /**
     * URL for verifying the credential.
     */
    url: string;
}

export interface ICareergoAccomplishment {
    /**
     * Accomplishment title.
     */
    title: string;
    /**
     * Details about the accomplishment.
     */
    description: string;
    /**
     * Date of the accomplishment.
     */
    date: string;
}

export interface ICareergoReferee {
    /**
     * Referee's name.
     */
    name: string;
    /**
     * Referee's job title.
     */
    position: string;
    /**
     * Referee's organization.
     */
    company: string;
    /**
     * Referee's email address.
     */
    email: string;
    /**
     * Referee's phone number.
     */
    phone: string;
}

export interface ICareergoLanguage {
    /**
     * Language spoken or written.
     */
    language_name: string;
    /**
     * Candidate's skill level in the language.
     */
    proficiency: string;
    /**
     * Language certification, if any.
     */
    certification: string;
}

export interface ICareergoProfessionalDevelopment {
    /**
     * Course or training name.
     */
    course_name: string;
    /**
     * Organization that provided the course.
     */
    institution: string;
    /**
     * Date the course was completed or issued.
     */
    issued_date: string;
    /**
     * Certificate identifier.
     */
    certificate_id: string;
    /**
     * Course content or skills gained.
     */
    description: string;
}

export interface ICareergoProfessionalMembership {
    /**
     * Professional organization name.
     */
    organization_name: string;
    /**
     * Membership identifier.
     */
    membership_number: string;
    /**
     * Membership category.
     */
    membership_type: string;
    /**
     * Membership start date.
     */
    member_since: string;
    /**
     * Membership expiry date.
     */
    expires_on: string;
    /**
     * Additional membership details.
     */
    notes: string;
}

export interface ICareergoVolunteerExperience {
    /**
     * Organization where the candidate volunteered.
     */
    organization: string;
    /**
     * Volunteer role.
     */
    role: string;
    /**
     * Volunteer work start date.
     */
    start_date: string;
    /**
     * Volunteer work end date.
     */
    end_date: string;
    /**
     * Volunteer responsibilities or results.
     */
    description: string;
}

/**
 * Section identifiers accepted by Careergo's resume designer.
 */
export const CAREERGO_RESUME_SECTIONS = [
    "professionalSummary",
    "workExperience",
    "education",
    "projects",
    "certifications",
    "accomplishments",
    "professionalDevelopment",
    "professionalMemberships",
    "volunteerExperience",
    "languages",
    "skills",
    "referees"
] as const;

/**
 * Careergo resume section identifier.
 */
export type CareergoResumeSection = (typeof CAREERGO_RESUME_SECTIONS)[number];

/**
 * Default section order from Careergo's Classic template.
 */
export const CAREERGO_RESUME_DEFAULT_SECTION_ORDER: CareergoResumeSection[] = [...CAREERGO_RESUME_SECTIONS];

/**
 * Resume data shape used by Careergo's document templates.
 */
export interface ICareergoResume {
    /**
     * Candidate's contact details.
     */
    personalDetails: ICareergoPersonalDetails;
    /**
     * Short overview of the candidate's professional background.
     */
    professionalSummary: string;
    /**
     * Candidate's work history.
     */
    employments: ICareergoEmployment[];
    /**
     * Candidate's education history.
     */
    education: ICareergoEducation[];
    /**
     * Skills listed as Careergo expects them.
     */
    skills: string;
    /**
     * Selected projects.
     */
    projects: ICareergoProject[];
    /**
     * Professional certifications.
     */
    certifications: ICareergoCertification[];
    /**
     * Awards and other accomplishments.
     */
    accomplishments: ICareergoAccomplishment[];
    /**
     * Professional referees.
     */
    referees: ICareergoReferee[];
    /**
     * Languages and skill levels.
     */
    languages: ICareergoLanguage[];
    /**
     * Courses and other professional development.
     */
    professionalDevelopment: ICareergoProfessionalDevelopment[];
    /**
     * Professional association memberships.
     */
    professionalMemberships: ICareergoProfessionalMembership[];
    /**
     * Volunteer roles.
     */
    volunteerExperience: ICareergoVolunteerExperience[];
    /**
     * Order used to render resume sections in preview and exported documents.
     */
    sectionOrder?: CareergoResumeSection[];
}

const personalDetailsSchema = z.object({
    firstName: z.string(),
    lastName: z.string(),
    email: z.string(),
    phoneNumber: z.string(),
    address: z.string(),
    city: z.string(),
    state: z.string(),
    country: z.string(),
    postalCode: z.string(),
    linkedin: z.string(),
    website: z.string()
});
const employmentSchema = z.object({
    jobTitle: z.string(),
    employer: z.string(),
    startDate: z.string(),
    endDate: z.string(),
    isPresent: z.boolean(),
    achievements: z.array(z.string()),
    city: z.string(),
    state: z.string(),
    country: z.string()
});
const educationSchema = z.object({
    degree: z.string(),
    institution: z.string(),
    startDate: z.string(),
    endDate: z.string(),
    isPresent: z.boolean(),
    city: z.string(),
    state: z.string(),
    country: z.string()
});
const projectSchema = z.object({
    name: z.string(),
    description: z.string(),
    url: z.string(),
    startDate: z.string(),
    endDate: z.string(),
    technologies: z.array(z.string())
});
const certificationSchema = z.object({
    name: z.string(),
    issuer: z.string(),
    issueDate: z.string(),
    expiryDate: z.string(),
    credentialId: z.string(),
    url: z.string()
});
const accomplishmentSchema = z.object({ title: z.string(), description: z.string(), date: z.string() });
const refereeSchema = z.object({
    name: z.string(),
    position: z.string(),
    company: z.string(),
    email: z.string(),
    phone: z.string()
});
const languageSchema = z.object({ language_name: z.string(), proficiency: z.string(), certification: z.string() });
const professionalDevelopmentSchema = z.object({
    course_name: z.string(),
    institution: z.string(),
    issued_date: z.string(),
    certificate_id: z.string(),
    description: z.string()
});
const professionalMembershipSchema = z.object({
    organization_name: z.string(),
    membership_number: z.string(),
    membership_type: z.string(),
    member_since: z.string(),
    expires_on: z.string(),
    notes: z.string()
});
const volunteerExperienceSchema = z.object({
    organization: z.string(),
    role: z.string(),
    start_date: z.string(),
    end_date: z.string(),
    description: z.string()
});

/**
 * Validates a resume against the shape Careergo's templates consume.
 */
export const careergoResumeSchema: z.ZodType<ICareergoResume> = z.object({
    personalDetails: personalDetailsSchema,
    professionalSummary: z.string(),
    employments: z.array(employmentSchema),
    education: z.array(educationSchema),
    skills: z.string(),
    projects: z.array(projectSchema),
    certifications: z.array(certificationSchema),
    accomplishments: z.array(accomplishmentSchema),
    referees: z.array(refereeSchema),
    languages: z.array(languageSchema),
    professionalDevelopment: z.array(professionalDevelopmentSchema),
    professionalMemberships: z.array(professionalMembershipSchema),
    volunteerExperience: z.array(volunteerExperienceSchema),
    sectionOrder: z.array(z.enum(CAREERGO_RESUME_SECTIONS)).optional()
});
