<script lang="ts">
    import Button from "#lib/components/Button.svelte";
    import CircleBackground from "#lib/components/CircleBackground.svelte";
    import Container from "#lib/components/Container.svelte";
    import KwelaDemo from "#lib/components/KwelaDemo.svelte";
    import PhoneFrame from "#lib/components/PhoneFrame.svelte";

    const steps = [
        { title: "Share the job", text: "Send Kwela the job as a link, pasted text or a photo of the poster, along with your CV. A photo of your CV works too." },
        { title: "Answer a couple of questions", text: "Kwela texts like a person. She asks one quick thing at a time, only when she needs it to write something true about you." },
        { title: "Read, change, send", text: "Your CV, cover letter and application answers land in the chat. Tap any line to change it, then send it yourself." }
    ];
    const good = [
        "Your chats and documents are saved on your phone.",
        "Private-test access is limited to invited people. Ask to join on the early-access page.",
        "You can delete your account and data at any time, in the app or on the deletion page."
    ];
</script>

<svelte:head>
    <title>Kwela: your CV writer, on your phone</title>
    <meta name="description" content="Share a job and your CV, and Kwela writes a tailored CV, cover letter and application answers for you to review and send." />
</svelte:head>

<div class="overflow-hidden py-16 sm:py-28 lg:pb-32 xl:pb-36">
    <Container>
        <div class="lg:grid lg:grid-cols-12 lg:gap-x-8 lg:gap-y-20">
            <div class="relative z-10 mx-auto max-w-2xl lg:col-span-7 lg:max-w-none lg:pt-6 xl:col-span-6">
                <h1 class="text-4xl font-medium tracking-tight text-gray-900 sm:text-5xl">Your CV writer, on your phone.</h1>
                <p class="mt-6 text-lg text-gray-600">
                    Share a job and your CV. Kwela writes a tailored CV, a cover letter and answers to the application questions. You read them, change what you
                    like, and send them yourself.
                </p>
                <div class="mt-8 flex flex-wrap gap-x-6 gap-y-4">
                    <Button href="/join" color="teal" class="px-5 py-3 text-base">Try Kwela early</Button>
                    <Button href="/#how" variant="outline" class="px-5 py-3 text-base">See how it works</Button>
                </div>
                <p class="mt-6 max-w-md text-sm text-gray-500">Kwela never sends an application for you. You stay in charge of what goes out.</p>
            </div>
            <div class="relative mt-14 sm:mt-20 lg:col-span-5 lg:row-span-2 lg:mt-0 xl:col-span-6">
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
                    <PhoneFrame class="mx-auto max-w-[366px]"><KwelaDemo /></PhoneFrame>
                </div>
            </div>
        </div>
    </Container>
</div>

<section id="how" class="bg-teal-600 py-20 sm:py-32">
    <Container>
        <div class="mx-auto max-w-2xl md:text-center">
            <h2 class="text-3xl font-medium tracking-tight text-white sm:text-4xl">The job lands in a chat. Your application leaves in minutes.</h2>
            <p class="mt-4 text-lg text-teal-50">Most jobs reach you in a WhatsApp group or an Instagram post. Kwela starts from there.</p>
        </div>
        <ol class="mx-auto mt-14 grid max-w-2xl gap-8 lg:mx-0 lg:max-w-none lg:grid-cols-3">
            {#each steps as step, index}
                <li class="rounded-4xl bg-white/10 p-8 ring-1 ring-white/20">
                    <span class="flex h-10 w-10 items-center justify-center rounded-full bg-white text-base font-semibold text-teal-800">{index + 1}</span>
                    <h3 class="mt-6 text-lg font-semibold text-white">{step.title}</h3>
                    <p class="mt-2 text-base text-teal-50">{step.text}</p>
                </li>
            {/each}
        </ol>
    </Container>
</section>

<section class="py-20 sm:py-28">
    <Container>
        <div class="mx-auto max-w-2xl">
            <h2 class="text-3xl font-medium tracking-tight text-gray-900">Good to know</h2>
            <ul class="mt-6 space-y-4">
                {#each good as line}
                    <li class="flex gap-3 text-base text-gray-700">
                        <svg viewBox="0 0 24 24" fill="none" aria-hidden="true" class="mt-0.5 h-6 w-6 flex-none"><circle cx="12" cy="12" r="11" class="fill-teal-100" /><path d="M7.5 12.5l3 3 6-7" class="stroke-teal-700" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
                        <span>{line}</span>
                    </li>
                {/each}
            </ul>
            <p class="mt-6 text-sm text-gray-500">Read our <a href="/privacy" class="text-teal-700 underline underline-offset-4">Privacy Policy</a> and <a href="/terms" class="text-teal-700 underline underline-offset-4">Terms of Service</a>.</p>
        </div>
    </Container>
</section>

<section class="relative overflow-hidden bg-gray-900 py-20 sm:py-28">
    <div class="absolute top-1/2 left-20 -translate-y-1/2 sm:left-1/2 sm:-translate-x-1/2">
        <CircleBackground id="cta-circle" color="#fff" class="animate-spin-slower" />
    </div>
    <Container class="relative">
        <div class="mx-auto max-w-md sm:text-center">
            <h2 class="text-3xl font-medium tracking-tight text-white sm:text-4xl">Try Kwela before everyone else</h2>
            <p class="mt-4 text-lg text-gray-300">Leave your email and phone number. We will invite you to the private test and ask how you look for jobs today.</p>
            <div class="mt-8 flex justify-center"><Button href="/join" color="white" class="px-6 py-3 text-base">Try Kwela early</Button></div>
        </div>
    </Container>
</section>
