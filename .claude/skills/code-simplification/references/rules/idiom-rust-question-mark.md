---
title: Use ? for Error Propagation
impact: LOW-MEDIUM
tags: idiom, rust, error-handling, question-mark-operator
---

## Use ? for Error Propagation

`?` is Rust's idiomatic way to propagate errors up the call stack. Replaces verbose nested `match` with concise syntax. Happy path stays obvious; errors stay explicit.

**Incorrect (nested match obscures logic):**

```rust
fn read_config(path: &str) -> Result<Config, ConfigError> {
    let file = match File::open(path) {
        Ok(f) => f,
        Err(e) => return Err(ConfigError::IoError(e)),
    };

    let mut contents = String::new();
    match file.read_to_string(&mut contents) {
        Ok(_) => {},
        Err(e) => return Err(ConfigError::IoError(e)),
    };

    match serde_json::from_str(&contents) {
        Ok(config) => Ok(config),
        Err(e) => Err(ConfigError::ParseError(e)),
    }
}
```

**Correct (? for clean propagation):**

```rust
fn read_config(path: &str) -> Result<Config, ConfigError> {
    let mut file = File::open(path)?;
    let mut contents = String::new();
    file.read_to_string(&mut contents)?;
    let config = serde_json::from_str(&contents)?;
    Ok(config)
}

// Even cleaner with method chaining
fn read_config_chained(path: &str) -> Result<Config, ConfigError> {
    let contents = std::fs::read_to_string(path)?;
    Ok(serde_json::from_str(&contents)?)
}
```

### When to Use match Instead

- Specific error variants need different handling
- Error needs added context or transformation
- Recoverable error you don't want to propagate

```rust
// match is appropriate here - different handling per error
fn connect_with_retry(addr: &str) -> Result<Connection, Error> {
    match TcpStream::connect(addr) {
        Ok(stream) => Ok(Connection::new(stream)),
        Err(e) if e.kind() == ErrorKind::ConnectionRefused => {
            thread::sleep(Duration::from_secs(1));
            connect_with_retry(addr) // Retry on refused
        }
        Err(e) => Err(e.into()), // Propagate other errors
    }
}
```

### Benefits

- Works with `Result` and `Option`
- Pairs with `From` trait for automatic error conversion
