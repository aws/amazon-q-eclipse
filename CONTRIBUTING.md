# Contributing Guidelines

Thank you for your interest in contributing to our project. Whether it's a bug report, new feature, correction, or additional 
documentation, we greatly value feedback and contributions from our community.

Please read through this document before submitting any issues or pull requests to ensure we have all the necessary 
information to effectively respond to your bug report or contribution.

## Reporting Bugs/Feature Requests

We welcome you to use the GitHub issue tracker to report bugs or suggest features.

When filing an issue, please check [existing open](https://github.com/aws/amazon-q-eclipse/issues), or [recently closed](https://github.com/aws/amazon-q-eclipse/issues?utf8=%E2%9C%93&q=is%3Aissue%20is%3Aclosed%20), issues to make sure somebody else hasn't already 
reported the issue. Please try to include as much information as you can. Details like these are incredibly useful:

* A reproducible test case or series of steps
* The version of the plugin being used, which Eclipse IDE product being used (and version)
* Anything unusual about your environment (e.g. recently installed plugins etc.)

## Building From Source

### Requirements
* [Java 17](https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/downloads-list.html)
* [Maven](https://maven.apache.org/download.cgi)
* [Git](https://git-scm.com/)

### Instructions

1. Clone this GitHub repository.
2. From the root of the workspace, run `mvn package`
3. Find the resulting plugin JAR: `plugin/target/amazon-q-eclipse-<version>-SNAPSHOT.jar`
4. Copy this file to your Eclipse installation's `dropins` folder. The location is platform dependent. On Windows, it is typically found at `<eclipse_install_dir>\dropins`. On Mac, `Eclipse.app/Contents/Eclipse/dropins` (use "Show Package Contents" to open the app). On Linux, `<eclipse_install_dir>/dropins`.
5. If it was running, close your IDE. Launch Eclipse and the plugin should be running. Note: make sure additional versions of the plugin are not installed to avoid conflicts.

## Contributing via Pull Requests

Contributions via pull requests are much appreciated. Before sending us a pull request, please ensure that:

1. You are working against the latest source on the *main* branch.
2. You check existing open, and recently merged, pull requests to make sure someone else hasn't addressed the problem already.
3. You open an issue to discuss any significant work - we would hate for your time to be wasted.

To send us a pull request, please:

1. Fork the repository
2. Modify the source; please focus on the specific change you are contributing. *(note: all changes must have associated automated tests)*
3. Ensure local tests pass by running:
   ```
   ./mvn package
   ```

4. Commit to your fork using clear commit messages. Again, reference the Issue # if relevant.
5. Send us a pull request by completing the pull-request template.
6. Pay attention to any automated build failures reported in the pull request.
7. Stay involved in the conversation.

GitHub provides additional documentation on [forking a repository](https://help.github.com/articles/fork-a-repo/) and 
[creating a pull request](https://help.github.com/articles/creating-a-pull-request/).

## Updating Plugin Version

To update the plugin version across all files:

1. **Prerequisites**: Ensure you have Git Bash (Windows) or Terminal (Mac/Linux)
2. **Run the version script**:
   ```bash
   ./update-version.sh [new-version]
   ```
   Example: `./update-version.sh 2.7.0`

3. **What gets updated**:
   - Root `pom.xml` version
   - All child `pom.xml` parent versions
   - `plugin/META-INF/MANIFEST.MF` Bundle-Version
   - `feature/feature.xml` version
   - `updatesite/category.xml` version references

4. **Build with new version**: `mvn clean install` and `mvn clean package`

## Debugging/Running Locally
To test your changes locally, you can run the plugin from your workspace by importing it into Eclipse.

1. First, make sure you have the [Eclipse IDE for Enterprise Java and Web Developers](https://www.eclipse.org/downloads/packages/) version of Eclipse installed which has all necessary dependencies to build and run the plugin.
2. From Eclipse, `File->Import...` and select `Maven->Existing Maven Projects`.
3. Select the checked out plugin repository folder.
4. You should see multiple projects identified. For plugin changes, you only need to select the `plugin/pom.xml` sub project. Leave the others unchecked.
5. This should import the project into your workspace. Eclipse will automatically build changes as you make them. To run the project, select `Run As->Eclipse Application` from the `Run` menu. This will start an Eclipse instance with your local plugin instance running.


## Code of Conduct

This project has adopted the [Amazon Open Source Code of Conduct](https://aws.github.io/code-of-conduct). 
For more information see the [Code of Conduct FAQ](https://aws.github.io/code-of-conduct-faq) or contact 
[opensource-codeofconduct@amazon.com](mailto:opensource-codeofconduct@amazon.com) with any additional questions or comments.

## Licensing

See the [LICENSE](LICENSE) file for our project's licensing. We will ask you confirm the licensing of your contribution.

We may ask you to sign a [Contributor License Agreement (CLA)](http://en.wikipedia.org/wiki/Contributor_License_Agreement) for larger changes.
