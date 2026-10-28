<script lang="ts">
    import Button from "#lib/components/Button.svelte";
    import CircleBackground from "#lib/components/CircleBackground.svelte";
    import Container from "#lib/components/Container.svelte";
    import DeviceIcon from "#lib/components/DeviceIcon.svelte";
    import KwelaScreen from "#lib/components/KwelaScreen.svelte";
    import PhoneFrame from "#lib/components/PhoneFrame.svelte";

    const sources = ["WhatsApp groups", "Instagram posts", "Emails", "Job websites"];

    const features = [
        {
            name: "Apply straight from where you found the job",
            description: "Share a poster photo, a link or a pasted message from WhatsApp, Instagram or your email. No saving it, retyping it or hunting for the details.",
            icon: "share" as const,
            scene: "share" as const
        },
        {
            name: "Get a CV that sounds like you, not like a template",
            description: "Kwela asks one quick question at a time, and only when she needs it, so your CV says specific, true things about you. Tap a ready answer or type your own.",
            icon: "pen" as const,
            scene: "ask" as const
        },
        {
            name: "Leave with the whole application, ready to send",
            description: "Your CV, cover letter and application answers land in the chat. Open them, tap any line to change it, then download or share them as PDFs.",
            icon: "pdf" as const,
            scene: "docs" as const
        }
    ];
    let selected = $state(0);

    const benefits = [
        { name: "A CV that matches each job", description: "Every CV is rewritten around the job you are applying for, so it speaks the employer's language.", icon: "match" as const },
        { name: "Answers in your own words", description: "Tap any line and change it until it sounds like you.", icon: "pen" as const },
        { name: "Photos are fine", description: "Send a photo of the job poster, or of your old CV. Kwela reads it for you.", icon: "photo" as const },
        { name: "PDFs you can send anywhere", description: "Download or share your CV and cover letter as PDFs, straight from the chat.", icon: "pdf" as const },
        { name: "You decide what goes out", description: "Kwela never sends an application for you. You read it, then send it yourself.", icon: "share" as const },
        { name: "Your chats stay with you", description: "Chats and documents live on your phone, and you can delete your account and data at any time.", icon: "lock" as const }
    ];

    const faqs = [
        [
            { question: "Is Kwela free?", answer: "Invited testers use Kwela free during the private test, within fair-use limits." },
            { question: "Does Kwela send applications for me?", answer: "No. You read everything, change what you like and send it yourself, so nothing goes out that you have not approved." }
        ],
        [
            { question: "What can I send Kwela?", answer: "The job as a link, pasted text or a photo, and your CV as a PDF, Word file, text file or photo. Each file can be up to 5 MB." },
            { question: "Do I need a password?", answer: "No. You sign in with a 6-digit code sent to your email, or with your Google account." }
        ],
        [
            { question: "Where are my chats kept?", answer: "On your phone. Your CV profile is also saved on our server so you do not have to upload it again for every job." },
            { question: "How do I delete my data?", answer: "In the app, open your profile, then Settings, then Delete account and data. You can also use the deletion page on this site." }
        ]
    ];
</script>

<svelte:head>
    <title>Kwela: apply to the job you just found, in minutes</title>
    <meta name="description" content="Share a job from WhatsApp, Instagram or email. Kwela writes a CV and cover letter that fit it, plus answers to the application questions, for you to read and send." />
</svelte:head>

<!-- Hero -->
<div class="overflow-hidden py-20 sm:py-32 lg:pb-32 xl:pb-36">
    <Container>
        <div class="lg:grid lg:grid-cols-12 lg:gap-x-8 lg:gap-y-20">
            <div class="relative z-10 mx-auto max-w-2xl lg:col-span-7 lg:max-w-none lg:pt-6 xl:col-span-6">
                <h1 class="text-4xl font-medium tracking-tight text-gray-900">Apply to the job you just found, in minutes.</h1>
                <p class="mt-6 text-lg text-gray-600">
                    Share the poster, link or message from WhatsApp or Instagram. Kwela writes a CV and cover letter that fit the job, plus answers to the application questions.
                    You read, tweak and send.
                </p>
                <div class="mt-8 flex flex-wrap gap-x-6 gap-y-4">
                    <Button href="/join" class="px-4 py-2.5">Try Kwela early</Button>
                    <Button href="/#features" variant="outline" class="px-4 py-2.5">See how it works</Button>
                </div>
            </div>
            <div class="relative mt-10 sm:mt-20 lg:col-span-5 lg:row-span-2 lg:mt-0 xl:col-span-6">
                <svg viewBox="0 0 1026 1026" fill="none" aria-hidden="true" class="absolute top-4 left-1/2 h-[1026px] w-[1026px] -translate-x-1/3 mask-[linear-gradient(to_bottom,white_20%,transparent_75%)] stroke-gray-300/70 sm:top-16 sm:-translate-x-1/2 lg:-top-16 lg:ml-12 xl:-top-14 xl:ml-0">
                    <g class="origin-center animate-spin-slow">
                        <path d="M1025 513c0 282.77-229.23 512-512 512S1 795.77 1 513 230.23 1 513 1s512 229.23 512 512Z" stroke-opacity="0.7" />
                        <path d="M513 1025C230.23 1025 1 795.77 1 513" stroke="#18aa9d" stroke-linecap="round" />
                    </g>
                    <g class="origin-center animate-spin-reverse-slower">
                        <path d="M913 513c0 220.914-179.086 400-400 400S113 733.914 113 513s179.086-400 400-400 400 179.086 400 400Z" stroke-opacity="0.7" />
                        <path d="M913 513c0 220.914-179.086 400-400 400" stroke="#18aa9d" stroke-linecap="round" />
                    </g>
                </svg>
                <div class="-mx-4 h-[448px] mask-[linear-gradient(to_bottom,white_60%,transparent)] px-9 sm:mx-0 lg:absolute lg:-inset-x-10 lg:-top-10 lg:-bottom-20 lg:h-auto lg:px-0 lg:pt-10 xl:-bottom-32">
                    <PhoneFrame class="mx-auto max-w-[366px]"><KwelaScreen scene="docs" animate /></PhoneFrame>
                </div>
            </div>
            <div class="relative mt-10 lg:col-span-7 lg:mt-0 xl:col-span-6">
                <p class="text-center text-sm font-semibold text-gray-900 lg:text-left">Works with jobs from</p>
                <ul role="list" class="mx-auto mt-8 flex max-w-xl flex-wrap justify-center gap-x-10 gap-y-6 lg:mx-0 lg:justify-start">
                    {#each sources as source}
                        <li class="text-lg font-semibold tracking-tight text-gray-500">{source}</li>
                    {/each}
                </ul>
            </div>
        </div>
    </Container>
</div>

<!-- Primary features -->
<section id="features" aria-label="How Kwela helps you apply" class="bg-gray-900 py-20 sm:py-32">
    <Container>
        <div class="mx-auto max-w-2xl lg:mx-0 lg:max-w-3xl">
            <h2 class="text-3xl font-medium tracking-tight text-white">From job post to finished application, all in one chat.</h2>
            <p class="mt-2 text-lg text-gray-400">
                Everything between finding a job and pressing send happens in a conversation, so you never need a cyber café, a template or ten browser tabs.
            </p>
        </div>

        <div class="mt-16 grid items-center gap-8 md:mt-20 md:grid-cols-12 lg:gap-16 xl:gap-24">
            <div class="relative z-10 order-last space-y-6 md:col-span-6" role="group" aria-label="Pick a feature to see it on the phone">
                {#each features as feature, index}
                    <button
                        type="button"
                        aria-pressed={selected === index}
                        onclick={() => (selected = index)}
                        class="relative block w-full rounded-2xl p-8 text-left transition-colors {selected === index ? 'bg-gray-800' : 'hover:bg-gray-800/30'}"
                    >
                        <DeviceIcon kind={feature.icon} tone="dark" />
                        <h3 class="mt-6 text-lg font-semibold text-white">{feature.name}</h3>
                        <p class="mt-2 text-sm text-gray-400">{feature.description}</p>
                    </button>
                {/each}
            </div>
            <div class="relative md:col-span-6">
                <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2">
                    <CircleBackground id="features-circle" color="#13B5C8" class="animate-spin-slower" />
                </div>
                <PhoneFrame class="z-10 mx-auto w-full max-w-[366px]">
                    {#key selected}
                        <div class="animate-fade-in h-full min-h-0"><KwelaScreen scene={features[selected].scene} animate /></div>
                    {/key}
                </PhoneFrame>
            </div>
        </div>
    </Container>
</section>

<!-- Secondary features -->
<section id="benefits" aria-label="Why people use Kwela" class="py-20 sm:py-32">
    <Container>
        <div class="mx-auto max-w-2xl sm:text-center">
            <h2 class="text-3xl font-medium tracking-tight text-gray-900">Less stress with every application.</h2>
            <p class="mt-2 text-lg text-gray-600">Kwela handles the writing, so you keep your time and your nerve for the interview.</p>
        </div>
        <ul role="list" class="mx-auto mt-16 grid max-w-2xl grid-cols-1 gap-6 text-sm sm:mt-20 sm:grid-cols-2 md:gap-y-10 lg:max-w-none lg:grid-cols-3">
            {#each benefits as benefit}
                <li class="rounded-2xl border border-gray-200 p-8">
                    <DeviceIcon kind={benefit.icon} />
                    <h3 class="mt-6 font-semibold text-gray-900">{benefit.name}</h3>
                    <p class="mt-2 text-gray-700">{benefit.description}</p>
                </li>
            {/each}
        </ul>
    </Container>
</section>

<!-- Call to action -->
<section class="relative overflow-hidden bg-gray-900 py-20 sm:py-28">
    <div class="absolute top-1/2 left-20 -translate-y-1/2 sm:left-1/2 sm:-translate-x-1/2">
        <CircleBackground id="cta-circle" color="#fff" class="animate-spin-slower" />
    </div>
    <Container class="relative">
        <div class="mx-auto max-w-md sm:text-center">
            <h2 class="text-3xl font-medium tracking-tight text-white sm:text-4xl">Be one of the first to apply the easy way</h2>
            <p class="mt-4 text-lg text-gray-300">Leave your email and phone number. We will invite you to the private test and ask how you apply for jobs today.</p>
            <div class="mt-8 flex justify-center"><Button href="/join" color="white" class="px-4 py-2.5">Try Kwela early</Button></div>
        </div>
    </Container>
</section>

<!-- FAQs -->
<section id="faqs" aria-labelledby="faqs-title" class="border-t border-gray-200 py-20 sm:py-32">
    <Container>
        <div class="mx-auto max-w-2xl lg:mx-0">
            <h2 id="faqs-title" class="text-3xl font-medium tracking-tight text-gray-900">Frequently asked questions</h2>
            <p class="mt-2 text-lg text-gray-600">
                If you have anything else you want to ask, <a href="/support" class="text-gray-900 underline">send us a message</a>.
            </p>
        </div>
        <ul role="list" class="mx-auto mt-16 grid max-w-2xl grid-cols-1 gap-8 sm:mt-20 lg:max-w-none lg:grid-cols-3">
            {#each faqs as column}
                <li>
                    <ul role="list" class="space-y-10">
                        {#each column as faq}
                            <li>
                                <h3 class="text-lg/6 font-semibold text-gray-900">{faq.question}</h3>
                                <p class="mt-4 text-sm text-gray-700">{faq.answer}</p>
                            </li>
                        {/each}
                    </ul>
                </li>
            {/each}
        </ul>
    </Container>
</section>
