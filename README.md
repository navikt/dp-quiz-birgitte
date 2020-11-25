# dp-quiz-birgitte

## Komme i gang

Gradle brukes som byggverktøy og er bundlet inn.

`./gradlew build`

## Coding style

Vi bruker [`ktlint`](https://github.com/pinterest/ktlint) som linter og formatter for Kotlin.

### Konfigurere IntelliJ med ktlint sine regler

```
brew install ktlint
ktlint applyToIDEAProject
```

Bonus: Sette opp pre-commit hook:

```
ktlint installGitPreCommitHook
```

## Co-Authors

Siden vi praktiserer mye par- og mobprogrammering er det bra å legge på de man
jobber med som Co-Authors.

Installer [Co-Author](https://plugins.jetbrains.com/plugin/10952-co-author)
pluginen i IntelliJ.

Opprett en liste med commiters med:

```
git shortlog -es | cut -c8- > ~/.git_coauthors
```

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan rettes mot:

* André Roaldseth, andre.roaldseth@nav.no
* Eller en annen måte for omverden å kontakte teamet på

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #dagpenger.
