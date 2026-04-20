<!-- Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved. -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

<template>
    <div @keydown.enter="handleContinueClick">
        <div class="font-amazon" v-if="existConnections.length > 0">
            <div class="title bottom-small-gap">Connect with an existing account:</div>
            <div v-for="(connection, index) in this.existConnections" :key="index">
                <SelectableItem
                    @toggle="toggleItemSelection"
                    :isSelected="selectedLoginOption === connection.id"
                    :itemId="connection.id"
                    :login-type="this.connectionType(connection)"
                    :itemTitle="this.connectionDisplayedName(connection)"
                    :itemText="this.connectionTypeDescription(connection)"
                    class="bottom-small-gap"
                ></SelectableItem>
            </div>
        </div>

        <div class="title font-amazon welcome-header bottom-small-gap" v-if="existingLogin.id === -1">Welcome to Amazon Q</div>
        <div class="maintenance-banner font-amazon bottom-small-gap" v-if="existingLogin.id === -1">
            <span class="maintenance-banner__icon" aria-hidden="true">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path
                        d="M12 2L1 21h22L12 2zm0 5.3L19.5 19H4.5L12 7.3zM11 10v5h2v-5h-2zm0 6v2h2v-2h-2z"
                        fill="currentColor"
                    />
                </svg>
            </span>
            <span class="maintenance-banner__text">
                Amazon Q Developer is now in maintenance mode. New accounts are no longer available. Existing users can still sign in below.
                <a class="maintenance-banner__link" href="#" @click.prevent="handleLearnMoreClick">Learn more</a>
            </span>
        </div>
        <button
            class="create-account-button font-amazon bottom-small-gap"
            type="button"
            disabled
            aria-disabled="true"
            title="New account creation is no longer available"
            v-if="existingLogin.id === -1"
        >
            <span>Create New Account</span>
            <svg class="create-account-button__lock" width="12" height="12" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
                <path
                    d="M12 1a5 5 0 0 0-5 5v3H6a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V11a2 2 0 0 0-2-2h-1V6a5 5 0 0 0-5-5zm-3 5a3 3 0 0 1 6 0v3H9V6z"
                    fill="currentColor"
                />
            </svg>
        </button>
        <div class="existing-users-divider font-amazon bottom-small-gap" v-if="existingLogin.id === -1"><span>existing users</span></div>
        <SelectableItem
            @toggle="toggleItemSelection"
            :isSelected="selectedLoginOption === LoginOption.BUILDER_ID"
            :itemId="LoginOption.BUILDER_ID"
            :login-type="LoginOption.BUILDER_ID"
            :itemTitle="'Builder ID'"
            :itemText="'Sign in with your existing Builder ID.'"
            class="font-amazon bottom-small-gap"
        ></SelectableItem>
        <SelectableItem
            @toggle="toggleItemSelection"
            :isSelected="selectedLoginOption === LoginOption.ENTERPRISE_SSO"
            :itemId="LoginOption.ENTERPRISE_SSO"
            :login-type="LoginOption.ENTERPRISE_SSO"
            :itemTitle="'Identity Center'"
            :itemText="'Sign in through your organization\'s IAM Identity Center.'"
            class="font-amazon bottom-small-gap"
        ></SelectableItem>
        <button
            class="login-flow-button continue-button font-amazon"
            :disabled="selectedLoginOption === LoginIdentifier.NONE"
            v-on:click="handleContinueClick()"
            tabindex="-1"
        >
            Sign in with existing account
        </button>
    </div>
</template>

<script lang="ts">
import {defineComponent} from 'vue'
import SelectableItem from "./selectableItem.vue";
import {AwsBearerTokenConnection, BuilderId, ExistConnection, Feature, LoginIdentifier, SONO_URL, Stage} from "../../model";
import {AWS_BUILDER_ID_NAME, IDENTITY_CENTER_NAME} from "../../constants"

export default defineComponent({
    name: "loginOptions",
    components: {SelectableItem},
    props: {
        app: String
    },
    computed: {
        LoginIdentifier() {
            return LoginIdentifier
        },
        stage(): Stage {
            return this.$store.state.stage
        },
        feature(): Feature {
            return this.$store.state.feature
        },
        existConnections(): AwsBearerTokenConnection[] {
            return this.$store.state.existingConnections
        }
    },
    data() {
        return {
            app: this.app,
            existingLogin: { id: -1, text: '', title: '' },
            selectedLoginOption: LoginIdentifier.NONE as string,
            LoginOption: LoginIdentifier
        }
    },
    methods: {
        toggleItemSelection(itemId: string) {
            if (this.selectedLoginOption == itemId) return;
            this.selectedLoginOption = itemId
            window.telemetryApi.postClickEvent(itemId + "Option")
        },
        handleLearnMoreClick() {
            window.telemetryApi.postClickEvent("maintenanceLearnMoreLink")
            window.ideApi.postMessage({
                command: 'openUrl',
                params: { url: 'https://aws.amazon.com/q/developer/' }
            })
        },
        handleBackButtonClick() {
            this.$emit('backToMenu')
        },
        async handleContinueClick() {
            window.telemetryApi.postClickEvent("continueButton")
            if (this.selectedLoginOption === LoginIdentifier.BUILDER_ID) {
                this.$emit('login', new BuilderId())
            } else if (this.selectedLoginOption === LoginIdentifier.ENTERPRISE_SSO) {
                this.$emit('stageChanged', 'SSO_FORM')
            } else {
                // TODO: else ... is not precise
                // TODO: should pass the entire connection json obj instead of connection id only
                this.$emit('login', new ExistConnection(this.selectedLoginOption))
            }
        },
        // TODO: duplicates in toolkitOptions, should leverage model/LoginOption interface
        connectionType(connection: AwsBearerTokenConnection): LoginIdentifier {
            if (connection.startUrl === SONO_URL) {
                return LoginIdentifier.BUILDER_ID
            }

            return LoginIdentifier.ENTERPRISE_SSO
        },
        // TODO: duplicates in toolkitOptions, should leverage model/LoginOption interface
        connectionTypeDescription(connection: AwsBearerTokenConnection): string {
            if (connection.startUrl === SONO_URL) {
                return AWS_BUILDER_ID_NAME
            }

            return IDENTITY_CENTER_NAME
        },
        // TODO: duplicates in toolkitOptions, should leverage model/LoginOption interface
        connectionDisplayedName(connection: AwsBearerTokenConnection): string {
            return `${connection.startUrl}`
        }
    }
})
</script>

<style scoped lang="scss">
.welcome-header {
    text-align: center;
}

.maintenance-banner {
    display: flex;
    gap: 8px;
    align-items: flex-start;
    padding: 10px 12px;
    border-radius: 5px;
    background: rgba(234, 179, 8, 0.12);
    border: 1px solid rgba(234, 179, 8, 0.45);
    color: #e0b84d;
    font-size: 12px;
    line-height: 1.5;

    &__icon {
        flex-shrink: 0;
        display: inline-flex;
        align-items: center;
        margin-top: 2px;
        color: inherit;
    }

    &__text {
        flex: 1;
    }

    &__link {
        color: inherit;
        text-decoration: underline;
        cursor: pointer;
        font-weight: 500;
    }

    &__link:hover {
        opacity: 0.85;
    }
}

body.jb-light .maintenance-banner {
    background: rgba(180, 120, 10, 0.08);
    border-color: rgba(180, 120, 10, 0.55);
    color: #8a6d1c;
}

.create-account-button {
    width: 100%;
    height: 30px;
    border: none;
    border-radius: 4px;
    font-size: 13px;
    font-weight: bold;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    background-color: #3a3a3a;
    color: #8a8a8a;
    cursor: not-allowed;
    margin-bottom: 24px !important;

    &__lock {
        color: currentColor;
    }
}

body.jb-light .create-account-button {
    background-color: #d4d4d4;
    color: #7a7a7a;
}

.existing-users-divider {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 10px;
    color: #888;
    font-size: 12px;
    margin-bottom: 24px !important;

    &::before,
    &::after {
        content: '';
        flex: 1;
        height: 1px;
        background: #3c3c3c;
    }

    span {
        white-space: nowrap;
    }
}

body.jb-light .existing-users-divider {
    color: #666;

    &::before,
    &::after {
        background: #c8c8c8;
    }
}
</style>
