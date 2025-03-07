# Data at the Point of Care - Datadog Fork

This is a fork of the CMS Data at the Point of Care (DPC). It is modified to run with additional tooling in order to serve as a demo application for demonstrating Datadog public sector use cases. You can find the original user guide and documentation [here](README-orig.md). The guides listed in README.md are specific to the modifications made in this fork.

## Changes from the original dpc-app repo

1. Added this readme - Documents changes and instructions. Much of this info is distilled from the original documentation.
2. Disabled decryption of sensitive local secrets - CMS included encrypted files in the repo that contain sesitive secrets for local development. Since we cannot access their vault password, this has been disabled and we have refactored away from the need for these sensitive variables.

## Running locally

### Prerequisites

In order to build locally, you need to do some one-time setup.

1. Install JDK 11. This project only supports specific JDK versions. It must use either JDK 11 or 12.

    One way to manage install and manage multiple java versions on a mac is to install and configure `jenv`.

    ```
    brew install jenv

    echo 'eval "$(jenv init -)"' >> /Users/will.boyd/.zshrc
    ```

    Also, install JDK 11.

    ```
    brew install java11
    ```

    Switch jenv to use JDK 11.

    ```
    jenv local 11
    ```

    Restart your terminal.

    Enable the jenv maven and export plugins:

    ```
    jenv enable-plugin maven

    jenv enable-plugin export
    ```

    Restart your terminal again.

    To verify that everything is set up properly, check that $JAVA_HOME points to the right place. You can, of course, simply set JAVA_HOME yourself and skip all the `jenv` stuff if you don't mind managing all that manually.

    Install maven:

    ```
    brew install maven
    ```

### Build and run the application.
1. Build the base and backend images:

    ```
    make build-local
    ```

    Note that this may take a bit to complete, as it runs a lot of automated tests. **TODO: Figure out a way to skip the unit tests when they're not needed, without breaking the dependency on jacoco report bundling.**

    Note that this will build docker image for a bunch of backend stuff, and store them in the local image registry.

2. Build frontend images and run the application:
    This step will build frontend images, and will therefore take longer on the first run (or in any other situation where the images need to be rebuilt).

    ```
    make start-dpc
    ```