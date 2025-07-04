// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

// eslint-disable-next-line header/header
import { createApp } from 'vue'
import {createStore, Store} from 'vuex'
import HelloWorld from './components/root.vue'
import {AwsBearerTokenConnection, Feature, IdcInfo, Profile, Region, Stage, State} from "../model";
import {IdeClient} from "../ideClient";
import './assets/common.scss'

const app = createApp(HelloWorld, { app: 'AMAZONQ' })
const store = createStore<State>({
    state: {
        stage: 'START' as Stage,
        ssoRegions: [] as Region[],
        authorizationCode: undefined,
        redirectUrl: undefined,
        lastLoginIdcInfo: {
            startUrl: '',
            region: '',
        },
        feature: 'Q',
        cancellable: false,
        existingConnections: [] as AwsBearerTokenConnection[],
        profiles: [] as Profile[],
        selectedProfile: null as Profile|null
    },
    getters: {},
    mutations: {
        setStage(state: State, stage: Stage) {
            state.stage = stage
        },
        setSsoRegions(state: State, regions: Region[]) {
            state.ssoRegions = regions
        },
        setCancellable() {

        },
        setAuthorizationCode(state: State, code: string) {
            state.authorizationCode = code
        },
        setRedirectUrl(state: State, redirectUrl: string) {
            state.redirectUrl = redirectUrl
        },
        setFeature(state: State, feature: Feature) {
            state.feature = feature
        },
        setLastLoginIdcInfo(state: State, idcInfo: IdcInfo) {
            console.log('state idc info is updated')
            state.lastLoginIdcInfo.startUrl = idcInfo.startUrl
            state.lastLoginIdcInfo.region = idcInfo.region
        },
        setExistingConnections(state: State, connections: AwsBearerTokenConnection[]) {
            state.existingConnections = connections
        },
        setProfiles(state, profiles) {
            state.profiles = Array.isArray(profiles) ? profiles : profiles.profiles || [];
        },
        setSelectedProfile(state, profile: Profile) {
            state.selectedProfile = profile
        },
        reset(state: State) {
            state.stage = 'START'
            state.ssoRegions = []
            state.authorizationCode = undefined
            state.redirectUrl = undefined
            state.lastLoginIdcInfo = {
                startUrl: '',
                region: ''
            }
            state.profiles = []
            state.selectedProfile = null
        }
    },
    actions: {},
    modules: {},
})

window.ideClient = new IdeClient(store)
app.directive('autofocus', {
    // When the bound element is inserted into the DOM...
    mounted: function (el) {
        el.focus();
    }
});
app.use(store).mount('#app')
