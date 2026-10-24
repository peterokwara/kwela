import assert from "node:assert/strict";
import test from "node:test";
import { uploadedFileToAgentContent } from "../src/lib/server/services/uploadedFileInputService.ts";

test("PDF uploads are sent as plain input_file blocks without unsupported provider parameters", async () => {
    const data = "data:application/pdf;base64,JVBERi0x";
    const [block] = await uploadedFileToAgentContent({ id: "1", name: "cv.pdf", type: "application/pdf", data });
    assert.deepEqual(block, { type: "input_file", file: data, filename: "cv.pdf" });
});

test("text uploads are decoded into the prompt", async () => {
    const data = `data:text/plain;base64,${Buffer.from("Amina Wanjiru, Nurse").toString("base64")}`;
    const [block] = await uploadedFileToAgentContent({ id: "2", name: "cv.txt", type: "text/plain", data });
    assert.equal(block.type, "input_text");
    assert.match(block.text, /Amina Wanjiru, Nurse/);
});
