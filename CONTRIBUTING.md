# Development and Testing

This repository contains a mixed-language codebase.

For contributing to the two underlying tools implemented in Python see:

- [Python Contributing Guide](./python/CONTRIBUTING.md)

For contributing to the Scala command line wrapper and tools see:

- [Scala Contributing Guide](./scala/CONTRIBUTING.md)

# Performing a Release

1. Checkout locally the `HEAD` commit on `main`
2. Bump the Python package version with `poetry version #.#.# --directory python`
3. Bump the Scala package version in the `build.sc` with the same version
4. Commit the changes on main with a message like `chore: bump to #.#.#`
5. Tag the commit with `git tag #.#.#`
6. Push the commit and its tag with `git push --tags`
