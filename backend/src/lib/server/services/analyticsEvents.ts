/**
 * Every analytics event the server sends. Names are fixed strings in the form category:object_action, in lowercase snake case,
 * so they can be filtered and grouped. Never build a name from a variable; put the variable in a property.
 */
export const ANALYTICS_EVENTS = {
    userCreate: "account:user_create",
    userSignIn: "account:user_sign_in",
    userDelete: "account:user_delete",
    applicationGenerate: "chat:application_generate",
    runFail: "chat:run_fail",
    pdfRender: "document:pdf_render"
} as const;
