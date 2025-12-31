<!-- omit in toc -->
# Contributing to inet-time

Thank you for your interest in contributing to this project!  *Internet Time* is a mostly
dead standard, so there's unlikely to be many users of this library, and maintaining it
probably requires more time and effort than it's worth.  That said, contributions are welcome.

Please read the relevant sections below before making a contribution.

> If you *actually* use this library in your project, or just like implementing retro things,
> please consider doing one or more of the following:
> - Star the project
> - Tweet about it
> - Refer this project in your project's readme
> - Mention the project at local meetups and tell your friends/colleagues

<!-- omit in toc -->
## Table of Contents

- [I Have a Question](#i-have-a-question)
  - [I Want To Contribute](#i-want-to-contribute)
  - [Reporting Bugs](#reporting-bugs)
  - [Suggesting Enhancements](#suggesting-enhancements)
  - [Your First Code Contribution](#your-first-code-contribution)
  - [Improving The Documentation](#improving-the-documentation)
- [Styleguides](#styleguides)
  - [Commit Messages](#commit-messages)
- [Join The Project Team](#join-the-project-team)



## I Have a Question

> If you want to ask a question, we assume that you have read the available [Documentation]().

Before you ask a question, it is best to search for existing
[Issues](https://github.com/wrprice/inet-time/issues) that might help you.
In case you have found a suitable issue and still need clarification, you can write your
question in this issue. It is also advisable to search the internet for answers first.

If you then still feel the need to ask a question and need clarification, we recommend the following:

- Open an [Issue](https://github.com/wrprice/inet-time/issues/new).
- Provide as much context as you can about what you're running into.
- Provide project and platform versions (OS, Java JDK version, etc.), depending on what seems relevant.

We will then take care of the issue as soon as possible (in our spare time).


## I Want To Contribute

> ### Legal Notice <!-- omit in toc -->
> When contributing to this project, you must agree that you have authored 100% of the content,
> that you have the necessary rights to the content and that the content you contribute may be
> provided under the project licence.

Please don't use AI.

### Reporting Bugs

<!-- omit in toc -->
#### Before Submitting a Bug Report

A good bug report shouldn't leave others needing to chase you up for more information.
Therefore, we ask you to investigate carefully, collect information and describe the issue
in detail in your report. Please complete the following steps in advance to help us fix any
potential bug as fast as possible.

- Make sure that you are using the latest version.
- Determine if your bug is really a bug and not an error on your side e.g. using incompatible
  environment components/versions (Make sure that you have read the [documentation]().
  If you are looking for support, you might want to check [this section](#i-have-a-question)).
- To see if other users have experienced (and potentially already solved) the same issue you
  are having, check if there is not already a bug report existing for your bug or error in the
  [bug tracker](https://github.com/wrprice/inet-time/issues?q=label%3Abug).
- Collect information about the bug:
  - Stack trace
  - OS, Platform and Version (Windows, Linux, macOS, x86, ARM)
  - Version of the JDK, runtime environment, build tool, and anything else that seems relevant.
  - The time zone configuration in your development or runtime environment where the problem occurs.
  - Possibly your input and the output
  - Can you reliably reproduce the issue? And can you also reproduce it with older versions?

<!-- omit in toc -->
#### How Do I Submit a Good Bug Report?

> This software only interacts with date and time values, and it does not process any sensitive
> information or provide any security mechanisms.  As a result, we do not expect there to be
> any security-related issues to report.  However, if you believe you have discovered a security
> issue, please do not put sensitive information in the issue tracker.  Open a bug with limited
> (non-sensitive) information and *request that we contact you privately* to discuss.

We use GitHub issues to track bugs and errors. If you run into an issue with the project:

- Open an [Issue](https://github.com/wrprice/inet-time/issues/new).
- Explain the behavior you would expect and the actual behavior.
- Please provide as much context as possible and describe the *reproduction steps* that
  someone else can follow to recreate the issue on their own. This usually includes your
  code. For good bug reports you should isolate the problem and create a reduced test case.
- Provide the information you collected in the previous section.

Once it's filed:

- The project team will label the issue accordingly.
- A team member will try to reproduce the issue with your provided steps.
  If there are no reproduction steps or no obvious way to reproduce the issue, the team
  will ask you for those steps and your issue may not be addressed until they are reproduced.
- If the team is able to reproduce the issue then it is ready for a fix to be
  [implemented by someone](#your-first-code-contribution).

<!-- You might want to create an issue template for bugs and errors that can be used as a guide and that defines the structure of the information to be included. If you do so, reference it here in the description. -->


### Suggesting Enhancements

This section guides you through submitting an enhancement suggestion for inet-time,
**including completely new features and minor improvements to existing functionality**.
Following these guidelines will help maintainers and the community to understand your
suggestion and find related suggestions.

<!-- omit in toc -->
#### Before Submitting an Enhancement

- Make sure that you are using the latest version.
- Read the [documentation]() carefully and find out if the functionality is already covered,
  maybe by an individual configuration.
- Perform a [search](https://github.com/wrprice/inet-time/issues) to see if the enhancement
  has already been suggested. If it has, add a comment to the existing issue instead of
  opening a new one.
- Find out whether your idea fits with the scope and aims of the project.
  It's up to you to make a strong case to convince the project's developers of the merits of
  this feature. Keep in mind that we want features that will be useful to the majority of our
  users and not just a small subset.
- Compatibility with the Java Time API is a primary goal, and we discourage implementing
  features that are easily accomplished using the existing APIs.

<!-- omit in toc -->
#### How Do I Submit a Good Enhancement Suggestion?

Enhancement suggestions are tracked as [GitHub issues](https://github.com/wrprice/inet-time/issues).

- Use a **clear and descriptive title** for the issue to identify the suggestion.
- Provide a **step-by-step description of the suggested enhancement** in as many details as possible.
- **Describe the current behavior** and **explain which behavior you expected to see instead** and
  why. At this point you can also tell which alternatives do not work for you.
- **Explain why this enhancement would be useful** to most inet-time users. You may also want to
  point out the other projects that solved it better and which could serve as inspiration.

<!-- You might want to create an issue template for enhancement suggestions that can be used as a guide and that defines the structure of the information to be included. If you do so, reference it here in the description. -->

### Your First Code Contribution

See the [README](README.md) for basic project setup and build instructions.

- Code changes and additions **MUST** have associated unit tests (using JUnit 5 "Jupiter")
- The project has a high code coverage percentage, help us keep it that way!
- Do not tolerate compiler warnings
- SpotBugs warnings will fail the build, including those in test code.  Do not suppress
  these without justification of a false-positive.
- In short, `./gradle clean build` must run successfully.

### Improving The Documentation

Documentation is provided as Javadoc built by the standard Java Doclet during the
normal build process.  To edit the documentation, you will edit the specially-formatted
comments in the code itself.  This project uses the newer *Markdown*-style of documentation
comments.

## Styleguides
### Commit Messages

Each commit message should begin with a single line that succinctly summarizes the
changes in the commit.  Please add additional information in the body of the commit
message with a blank line separating the two, for example:

    Updated build dependencies

    Gradle from 9.0.0 to 9.1.2
    Foolib from 1.2.3 to 4.5.6
    ... etc.

<!-- omit in toc -->
## Attribution
This guide is based on the [contributing.md](https://contributing.md/generator)!
