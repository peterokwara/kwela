<script lang="ts">
    import { site } from "#lib/config.ts";
</script>

<svelte:head>
    <title>Privacy Policy · Kwela</title>
    <meta name="description" content="What Kwela collects, why, who we share it with, how long we keep it and how to delete it." />
</svelte:head>

<h1>Privacy Policy</h1>
<p class="muted">Last updated {site.lastUpdated}</p>

<p>
    This policy explains what the Kwela app and website ("Kwela", "we") collect about you, why, and what you can do about it. We have tried to say it
    in plain words. If anything is unclear, write to <a href="mailto:{site.supportEmail}">{site.supportEmail}</a>.
</p>

<h2>What we collect</h2>
<table>
    <thead><tr><th>Information</th><th>Why</th><th>Where it lives</th></tr></thead>
    <tbody>
        <tr>
            <td><strong>Your email address</strong></td>
            <td>To create your account and verify your sign-in request.</td>
            <td>Our database, and on your phone.</td>
        </tr>
        <tr>
            <td><strong>Sign-in codes and sessions</strong></td>
            <td>To check it is you. We store a keyed hash of each code and a hash of each session token. The associated email address is stored with the code.</td>
            <td>Our Cloudflare database. During the closed test, the app displays a fixed, non-expiring code only after an allow-listed address requests one; no email is sent. Each request creates a one-use sign-in challenge. Real email codes stop working after 10 minutes or after use. Expired code records may remain until replaced or the account is deleted. Session records stop working after 180 days and may remain until sign-out or account deletion.</td>
        </tr>
        <tr>
            <td><strong>Messages you send and files you attach</strong> (CVs, job descriptions, photos)</td>
            <td>To write your replies and documents. They pass through our Cloudflare Worker to OpenAI's API for processing.</td>
            <td>Your phone and OpenAI's abuse-monitoring logs. Kwela does not save chat text or uploaded file contents in its own database. OpenAI says API abuse-monitoring logs may contain prompts and responses and are kept for up to 30 days by default, with exceptions for legal or security needs. See <a href="https://developers.openai.com/api/docs/guides/your-data" rel="noreferrer">OpenAI's API data controls</a>.</td>
        </tr>
        <tr>
            <td><strong>Your CV profile</strong> (name, contact details, education, work history, skills that Kwela reads from your CV)</td>
            <td>So you do not have to upload your CV again for each job.</td>
            <td>Our database, until you delete your account. A copy is kept on your phone.</td>
        </tr>
        <tr>
            <td><strong>Documents Kwela writes for you</strong></td>
            <td>So you can read, share and save them.</td>
            <td>Your phone only.</td>
        </tr>
        <tr>
            <td><strong>Usage records</strong> (an internal account ID, the time of each request and its estimated cost)</td>
            <td>To enforce free-use limits and stop abuse.</td>
            <td>Our database.</td>
        </tr>
        <tr>
            <td><strong>Sign-in rate-limit keys</strong> (keyed hashes based on your email address and, when available, IP address)</td>
            <td>To limit repeated code requests and prevent abuse.</td>
            <td>Our database. The source email address and IP address are not stored in this rate-limit table, but rate-limit rows do not yet have an automatic deletion job.</td>
        </tr>
        <tr>
            <td><strong>Technical data</strong> (such as your IP address and request details)</td>
            <td>Our hosting provider handles these to deliver the service and keep it secure. We do not use them to profile you.</td>
            <td>Our hosting provider.</td>
        </tr>
    </tbody>
</table>
<p>We do not use advertising or analytics tools in the app, and we do not sell your information.</p>

<h2>Service providers</h2>
<p>These providers process information for us so Kwela can work. Google Play's Data safety rules treat transfers to service providers acting on a developer's behalf as collection, not sharing.</p>
<ul>
    <li>
        <strong>OpenAI</strong> receives your messages and attached files so its AI models can write replies and documents. OpenAI processes API data for the service and says API data is not used to train its models by default. See its <a href="https://developers.openai.com/api/docs/guides/your-data" rel="noreferrer">API data controls</a>.
    </li>
    <li>
        <strong>Cloudflare</strong> hosts Kwela's servers and database. Your requests and account data pass through its network. The closed-test code is returned only to allow-listed accounts in the app; no email service currently receives it.
    </li>
</ul>
<p>We may also disclose information if the law requires it.</p>

<h2>Notifications</h2>
<p>
    If you allow notifications, Kwela tells you when a reply you asked for is ready while you are away from the app. The notification shows Kwela's message, and you can reply or mark it as read from there.
    If your phone is locked and you have chosen to hide sensitive notification content, Android shows only "New message from Kwela". You can turn replies off in Settings, or mute Kwela in your phone's notification settings. We never send reminders or marketing.
</p>

<h2>How long we keep it</h2>
<ul>
    <li>The closed-test code does not expire, but each sign-in challenge can be used once. Real email codes stop working after 10 minutes or after use. Their hashed database records may remain until replaced or account deletion.</li>
    <li>A session stops working after 180 days, or earlier if you sign out or delete your account. Its hashed database record may remain until sign-out or account deletion.</li>
    <li>Your account and CV profile stay until you delete your account.</li>
    <li>
        Usage records are kept for as long as we need them to enforce limits. They use an internal account ID and stay after you delete your
        account so that deleting and re-creating an account cannot be used to get around those limits.
    </li>
    <li>Rate-limit records contain keyed hashes and request counts. They have no automatic deletion job yet.</li>
    <li>Chats and documents on your phone stay there until you delete them, delete your account in the app, or uninstall Kwela.</li>
</ul>

<h2>Your choices and rights</h2>
<p>
    Kenya's Data Protection Act, 2019 gives you rights that include access to your personal data, objection to processing, correction of false or misleading data, and deletion of false or misleading data. The Data Protection Regulations also set out how to request erasure. See the <a href="{site.regulator.url}" rel="noreferrer">Office of the Data Protection Commissioner's guidance</a> or email us for help.
</p>
<ul>
    <li><strong>Delete your account:</strong> in the app, open your profile, then Settings, then Delete account and data. Or use our <a href="/delete-account">deletion page</a> if you no longer have the app.</li>
    <li><strong>Anything else:</strong> email <a href="mailto:{site.supportEmail}">{site.supportEmail}</a> from the address on your account and we will reply.</li>
    <li>
        <strong>Complaints:</strong> you can complain to the {site.regulator.name}: <a href="{site.regulator.url}" rel="noreferrer">{site.regulator.url}</a>.
    </li>
</ul>

<h2>Where your data is processed</h2>
<p>
    Our service providers run servers in several countries, so your information may be processed outside Kenya. We use providers that apply their own security and data-protection commitments.
</p>

<h2>Security</h2>
<p>
    Connections to Kwela are encrypted. Sign-in codes and session tokens are stored in hashed form in the database. During the closed test, the fixed test code is available only to allow-listed email addresses through the app. No system is perfectly secure, so please use a device lock and keep your email account safe.
</p>

<h2>Children</h2>
<p>Kwela is for people aged 18 and over. We do not knowingly collect information from anyone younger. If you think a child has given us information, tell us and we will delete it.</p>

<h2>Changes</h2>
<p>If we change this policy in a way that matters, we will update the date above and tell you in the app.</p>

<h2>Contact</h2>
<p>Kwela · <a href="mailto:{site.supportEmail}">{site.supportEmail}</a></p>
