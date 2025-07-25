// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

// eslint-disable-next-line header/header
import {createApp} from 'vue'
import {createStore, Store} from 'vuex'
import root from './components/root.vue'
import {AwsBearerTokenConnection, Feature, IdcInfo, Region, Stage, State} from "../model";
import {IdeClient} from "../ideClient";
import './assets/common.scss'

const app = createApp(root, { app: 'TOOLKIT' })

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
        feature: 'awsExplorer',
        cancellable: false,
        existingConnections: [] as AwsBearerTokenConnection[],
        profiles : [],
        selectedProfile: null
    },
    getters: {},
    mutations: {
        setStage(state: State, stage: Stage) {
            state.stage = stage
        },
        setSsoRegions(state: State, regions: Region[]) {
            state.ssoRegions = regions
        },
        setAuthorizationCode(state: State, code: string | undefined) {
            state.authorizationCode = code
        },
        setFeature(state: State, feature: Feature) {
            state.feature = feature
        },
        setLastLoginIdcInfo(state: State, idcInfo: IdcInfo) {
            state.lastLoginIdcInfo.startUrl = idcInfo.startUrl
            state.lastLoginIdcInfo.region = idcInfo.region
        },
        setCancellable(state: State, cancellable: boolean) {
            state.cancellable = cancellable
        },
        setExistingConnections(state: State, connections: AwsBearerTokenConnection[]) {
            state.existingConnections = connections
        },
        reset(state: State) {
            state.stage = 'START'
            state.ssoRegions = []
            state.authorizationCode = undefined
            state.lastLoginIdcInfo = {
                startUrl: '',
                region: ''
            }
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
