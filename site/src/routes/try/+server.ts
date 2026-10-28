import { redirect } from "@sveltejs/kit";
import type { RequestHandler } from "./$types";

/** A short address for the bio link. It is a temporary redirect, so the bio link can be pointed somewhere else later. */
export const GET: RequestHandler = () => redirect(302, "/join");
