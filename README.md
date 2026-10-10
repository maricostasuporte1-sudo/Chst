# Creator Life 3D

An open-source Godot 4.7.2 learning project: a small first-person creator-life simulator, with keyboard/mouse and touch controls.

**Status:** early prototype (v0.1). Intended for learning, experimentation and community contributions. Not a production-ready mobile game.

## Features

- Procedurally generated 3D bedroom, simple interactions and a HUD.
- Record in-game videos to earn simulated views, followers and currency.
- Upgrade the PC and internet; sleep to restore energy.
- Local JSON save under Godot's `user://` path (no accounts, telemetry or online services).
- Keyboard/mouse input and basic on-screen Android controls.
- Debug Android APK export workflow using Godot 4.7.2.

## Getting started

1. Install **Godot 4.7.2 stable** from https://godotengine.org/download/archive/4.7.2-stable/.
2. Import `project.godot` through Godot Project Manager.
3. Press **F6** to run `main.tscn` or **F5** to run the project.
4. On desktop: **W/A/S/D** to move, **mouse** to look, **E** to interact, **Esc** to release/capture cursor.
5. On Android: use the on-screen arrows, drag on the right side to look, and tap **AÇÃO** to interact.

In-game actions: interact with the PC to create a video or buy an upgrade; interact with the bed to advance a day and restore energy.

## Repository structure

```text
project.godot                  Godot project settings
main.tscn                     Main scene
scripts/main.gd               Prototype gameplay and procedural scene
export_presets.cfg            Android debug export configuration
tests/smoke_test.gd           Headless Godot smoke test
.github/workflows/ci.yml      Automated import and smoke tests
.github/workflows/build-apk.yml  Debug APK artifact workflow
```

## Running checks

```bash
godot --headless --path . --editor --quit
godot --headless --path . --script res://tests/smoke_test.gd
```

Replace `godot` with the path to the Godot 4.7.2 binary if necessary. CI runs both checks on pull requests and pushes. Successful import and smoke tests do not constitute comprehensive gameplay or device testing.

## Android build

The `build-apk.yml` GitHub Actions workflow exports a **debug** APK artifact. Use `workflow_dispatch` or push to `main`, then retrieve artifacts from the Actions run. The debug APK is intended for local testing only, not Play Store distribution. Before any production publication, perform device testing, security review and a release-signing process under your own control.

## Contributing

Issues and pull requests are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md). Reproduce bugs with the Godot version, operating system, steps and expected/actual behavior. Priorities: reliable Android controls, automated gameplay tests, accessible UI, and reproducible exports.

## Scope, history and licenses

**This open-source project is Creator Life 3D only.** Earlier commits of this repository also contain unrelated *Agendivo* web files. Those files are not part of the current project and are **not covered by the MIT license for Creator Life 3D**; they may remain accessible in Git history. Do not copy or reuse them based on this license.

The current Creator Life 3D project is licensed under [MIT](LICENSE). Godot Engine and GitHub Actions dependencies retain their respective licenses. No VERTEX Propel source, assets, databases or algorithms are included.

## Sobre o projeto (PT-BR)

Projeto educacional open source em Godot 4.7.2, com simulação simples de vida de criador de conteúdo, suporte básico a Android e gravação de progresso local. A versão 0.1 é um protótipo; contribuições e correções são bem-vindas.
