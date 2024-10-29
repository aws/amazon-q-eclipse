// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.lsp.auth.model;

import java.util.List;

public class Profile extends Section {
    private List<String> kinds;
    private ProfileSettings settings;

    public final List<String> getProfileKinds() {
        return kinds;
    }

    public final void setProfileKinds(final List<String> kinds) {
        this.kinds = kinds;
    }

    public final ProfileSettings getProfileSettings() {
        return settings;
    }

    public final void setProfileSettings(final ProfileSettings settings) {
        this.settings = settings;
    }
}
