# Intended Repository Structure After Stage 0

```text
ReaxKey/
├── .github/
│   ├── workflows/
│   │   └── android-ci.yml
│   └── pull_request_template.md
├── app/
│   ├── src/main/AndroidManifest.xml
│   └── ...
├── ime/
├── core/
│   ├── model/
│   ├── data/
│   ├── media/
│   ├── search/
│   └── ui/
├── feature/
│   ├── onboarding/
│   ├── library/
│   └── settings/
├── testbed/
├── docs/
├── work/
├── AGENTS.md
├── PROJECT_STATE.md
├── README.md
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
└── gradlew / gradlew.bat
```

The coding agent creates code directories during Stage 0. Do not create fake empty module folders merely to look complete; Gradle modules must be valid and buildable when added.
