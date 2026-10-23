const form = document.querySelector("#search-form");
const keywordInput = document.querySelector("#keywords");
const hoursInput = document.querySelector("#hours");
const resultCount = document.querySelector("#result-count");
const status = document.querySelector("#status");
const jobList = document.querySelector("#job-list");
const themeToggle = document.querySelector("#theme-toggle");

syncThemeToggle();
themeToggle.addEventListener("click", () => {
    const nextTheme = document.documentElement.dataset.theme === "light" ? "dark" : "light";
    document.documentElement.dataset.theme = nextTheme;
    localStorage.setItem("internship-tracker-theme", nextTheme);
    syncThemeToggle();
});

initializeDogRain();

form.addEventListener("submit", (event) => {
    event.preventDefault();
    loadJobs();
});

function initializeDogRain() {
    const rainLayer = document.querySelector("#dog-rain");
    const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)");
    if (reducedMotion.matches) {
        return;
    }

    for (let index = 0; index < 10; index += 1) {
        const size = 46 + Math.random() * 18;
        const duration = 28 + Math.random() * 14;
        const dog = document.createElement("img");
        dog.className = "dog-rain__image";
        dog.src = "/dog.png";
        dog.alt = "";
        dog.draggable = false;
        dog.style.setProperty("--size", `${size}px`);
        dog.style.setProperty("--left", `${Math.random() * Math.max(0, window.innerWidth - size)}px`);
        dog.style.setProperty("--duration", `${duration}s`);
        dog.style.setProperty("--delay", `${-Math.random() * duration}s`);
        dog.style.setProperty("--drift", `${Math.random() * 56 - 28}px`);
        rainLayer.append(dog);
    }
}

function syncThemeToggle() {
    const isDark = document.documentElement.dataset.theme !== "light";
    themeToggle.setAttribute("aria-label", `Switch to ${isDark ? "light" : "dark"} mode`);
    themeToggle.querySelector(".theme-toggle__label").textContent = `${isDark ? "Light" : "Dark"} mode`;
}

async function loadJobs() {
    const keywords = keywordInput.value.split(",").map((keyword) => keyword.trim()).filter(Boolean);
    const params = new URLSearchParams({ hours: hoursInput.value });
    keywords.forEach((keyword) => params.append("keywords", keyword));

    jobList.replaceChildren();
    resultCount.textContent = "";
    status.textContent = "Loading internships…";
    try {
        const response = await fetch(`/api/internships?${params.toString()}`);
        if (!response.ok) {
            throw new Error(`Request failed with status ${response.status}`);
        }
        const jobs = await response.json();
        resultCount.textContent = `${jobs.length} ${jobs.length === 1 ? "job" : "jobs"}`;
        status.textContent = jobs.length ? "" : "No internships found for this search.";
        jobs.forEach((job) => jobList.append(createJobCard(job)));
    } catch (error) {
        status.textContent = "Could not load internships. Please try again.";
        console.error("Unable to load internships", error);
    }
}

function createJobCard(job) {
    const card = document.createElement("article");
    card.className = "job-card";

    const details = document.createElement("div");
    const title = document.createElement("h3");
    title.textContent = job.title || "Internship";
    details.append(title);

    const meta = document.createElement("div");
    meta.className = "job-meta";
    appendText(meta, job.company || "Company not listed");
    appendText(meta, job.location || "Location not listed");
    appendText(meta, formatPostedAt(job.postedAt));

    const source = document.createElement("span");
    source.className = "source-badge";
    source.textContent = job.source || "Unknown source";
    meta.append(source);
    details.append(meta);

    const apply = document.createElement("a");
    apply.className = "apply-link";
    apply.textContent = "Apply";
    apply.href = job.url;
    apply.target = "_blank";
    apply.rel = "noopener noreferrer";

    card.append(details, apply);
    return card;
}

function appendText(parent, text) {
    const item = document.createElement("span");
    item.textContent = text;
    parent.append(item);
}

function formatPostedAt(value) {
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? "Date unavailable" : `Posted ${date.toLocaleString()}`;
}

loadJobs();
