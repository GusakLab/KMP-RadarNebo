# Design Patterns Skill

> Teach your AI coding assistant to write clean, maintainable, and scalable software using proven **Gang of Four (GoF)** design patterns and **SOLID** principles.

Instead of generating tightly coupled or difficult-to-maintain code, this skill helps your AI make architectural decisions based on the concepts from **Alexander Shvets' _Dive Into Design Patterns_**.

---

## ✨ Features

- 🏗 Full coverage of all 23 GoF design patterns  
- 🎯 Strong emphasis on SOLID principles and clean architecture  
- 🔄 Refactors tightly coupled code into modular designs  
- 🧩 Suggests appropriate patterns based on real-world context  
- 📖 Based on industry-standard design pattern methodology  
- 🤖 Works through natural language prompts in AI coding assistants  

---

## 📚 Reference

This skill is based on:

**Dive Into Design Patterns** by **Alexander Shvets**

Official reference:
<https://refactoring.guru/design-patterns>

If you're learning design patterns or want to understand the reasoning behind AI-generated architecture, this is the recommended resource.

---

## 🚀 Installation

This skill can be used with AI coding assistants that support Markdown-based skills or prompt injection directories (e.g., Claude Code, Gemini CLI, Codex CLI, Qwen Code, etc.).

### Option 1 — Install via CLI (Recommended)

```bash id="cli-install"
DISABLE_TELEMETRY=1 npx skills@1.7.6 add ariaramin/design-patterns-skill
```

The skills CLI sends anonymous usage data unless `DISABLE_TELEMETRY=1` is set. Review the repository before installing.

---

### Option 2 — Install from GitHub Releases

1. Go to the **Releases** page of this repository  
2. Download the latest `.skill` file  
3. Import it into your AI assistant (if supported)

---

### Option 3 — Clone Repository

```bash id="git-clone"
git clone https://github.com/ariaramin/design-patterns-skill.git
```

Then copy or link the skill into your AI assistant’s skills directory.

---

### Manual Skill Placement

After cloning or downloading:

Place the skill folder in one of the following locations depending on your tool:

**Claude Code**
```text id="claude-path"
~/.claude/skills/design-patterns-skill/
```

**Project-based setup**
```text id="project-path"
your-project/
└── .claude/
    └── skills/
        └── design-patterns-skill/
```

**Other assistants**
Use the equivalent skills/prompt directory defined by your tool and ensure `SKILL.md` is inside the folder.

Restart your agent after installation.

---

### Other Compatible AI Agents

Many modern AI coding assistants support Markdown Skills or prompt libraries.

Simply place the repository (or at least `SKILL.md`) into the agent's configured **Skills** directory, or import the downloaded `.skill` package if the agent provides an import feature.

Refer to your agent's documentation for the exact location of its Skills folder.

---

## Verify the Installation

Start a new AI session and ask:

> What design patterns do you know from your installed skills?

or

> Refactor this code using the Strategy Pattern.

If the assistant responds using the guidance from **Dive Into Design Patterns**, the skill has been loaded successfully.

---

## Keeping the Skill Updated

If you cloned the repository:

```bash
git pull
```

If you installed via the Skills CLI:

```bash
DISABLE_TELEMETRY=1 npx skills@1.7.6 update
```

If you installed from Releases, download the latest `.skill` package and replace the existing one.

---

## 💡 Usage

Once installed, simply describe what you want your AI to build or refactor.

### Example prompts

```text
Refactor this code using the Strategy Pattern.
```

```text
Implement a Factory Method for creating payment providers.
```

```text
Review this architecture and suggest appropriate GoF patterns.
```

```text
Apply SOLID principles to reduce coupling in this module.
```

```text
Recommend the best design pattern for this use case and explain why.
```

---

## 📦 Included Topics

### Creational Patterns

- Factory Method
- Abstract Factory
- Builder
- Prototype
- Singleton

### Structural Patterns

- Adapter
- Bridge
- Composite
- Decorator
- Facade
- Flyweight
- Proxy

### Behavioral Patterns

- Chain of Responsibility
- Command
- Iterator
- Mediator
- Memento
- Observer
- State
- Strategy
- Template Method
- Visitor

### Software Design Principles

- SOLID
- Composition over inheritance
- Loose coupling
- Separation of concerns
- Encapsulation
- Dependency inversion
- Maintainability and extensibility best practices

---

## 🤝 Contributing

Contributions are welcome.

You can help by:

- Improving prompts and instructions
- Adding language-specific examples
- Enhancing documentation
- Fixing issues or expanding pattern guidance

1. Fork the repository.
2. Create a feature branch.
3. Commit your changes.
4. Open a Pull Request.

---

## 📄 License

This project is licensed under the MIT License.

See the `LICENSE` file for details.

---

## ⭐ Support

If you find this project useful:

- ⭐ Star the repository
- 🍴 Share it with other developers
- 🛠 Contribute improvements
- 💬 Report issues or suggest new features

Every contribution helps improve AI-generated software architecture.
