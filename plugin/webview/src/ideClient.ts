// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

import { profile } from "console";
import {Store} from "vuex";
import {IdcInfo, Region, Stage, State, BrowserSetupData, AwsBearerTokenConnection, Profile} from "./model";

export class IdeClient {
    constructor(private readonly store: Store<State>) {}

    // TODO: design and improve the API here

    prepareUi(state: BrowserSetupData) {
        console.log('browser is preparing UI with state ', state)
        this.store.commit('setStage', state.stage)
        this.store.commit('setSsoRegions', state.regions)
        this.updateLastLoginIdcInfo(state.idcInfo)
        this.store.commit("setCancellable", state.cancellable)
        this.store.commit("setFeature", state.feature)
        this.store.commit('setProfiles', state.profiles);
        const existConnections = state.existConnections.map(it => {
            return {
                sessionName: it.sessionName,
                startUrl: it.startUrl,
                region: it.region,
                scopes: it.scopes,
                id: it.id
            }
        })

        this.store.commit("setExistingConnections", existConnections)
        this.updateAuthorization(undefined)
        this.updateRedirectUrl(undefined)
    }

    handleProfiles(profilesData: { profiles: Profile[] }) {
        this.store.commit('setStage', 'PROFILE_SELECT')
        console.debug("received profile data")
        const availableProfiles: Profile[] = profilesData.profiles;
        this.store.commit('setProfiles', availableProfiles);
    }

    updateAuthorization(code: string | undefined) {
        this.store.commit('setAuthorizationCode', code)
        // TODO: mutage stage to AUTHing here probably makes life easier
    }

    updateLastLoginIdcInfo(idcInfo: IdcInfo) {
        this.store.commit('setLastLoginIdcInfo', idcInfo)
    }

    updateRedirectUrl(redirectUrl: string | undefined) {
        this.store.commit('setRedirectUrl', redirectUrl)
    }

    reset() {
        this.store.commit('setStage', 'START')
    }

    cancelLogin(): void {
        // this.reset()
        this.store.commit('setStage', 'START')
        window.ideApi.postMessage({ command: 'cancelLogin' })
    }
}
