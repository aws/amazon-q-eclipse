<!-- SPDX-License-Identifier: Apache-2.0 -->

<template>
    <div @keydown.enter="handleContinueClick">
        <div class="font-amazon" v-if="availableProfiles.length > 0">
            <!-- Title & Subtitle -->
            <div class="profile-header">
                <h2 class="title bottom-small-gap">Choose a Q Developer profile</h2>
                <p class="profile-subtitle">
						Your administrator has given you access to Q from multiple profiles. Choose the profile that meets your current working needs. You can change your profile at any time.                </p>
            </div>
            <!-- Profile List -->
            <div class="profile-list">
                <div
                    v-for="(profile, index) in availableProfiles"
                    :key="index"
                    class="profile-item bottom-small-gap"
                    :class="{ selected: selectedProfile?.arn === profile.arn }"
                    @click="toggleItemSelection(profile)"
                    tabindex="0"
                >
                    <div class="text">
                        <div class="profile-name">{{ profile.name }} - <span class="profile-region">{{ profile.identityDetails.region }}</span></div>
                        <div class="profile-id">Account: {{ profile.accountId }}</div>
                    </div>
                </div>
            </div>
            <!-- Continue Button -->
            <button
                class="login-flow-button continue-button font-amazon"
                :disabled="selectedProfile === null"
                v-on:click="handleContinueClick()"
                tabindex="-1"
            >
                Continue
            </button>
        </div>
    </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import { Profile } from '../../model'

export default defineComponent({
    name: 'ProfileSelection',
    props: {
        app: { type: String, default: '' }
    },
    data() {
        return {
            selectedProfile: null as (Profile | null),
            availableProfiles: [] as Profile[]
        }
    },
    mounted() {
      console.debug("Vuex raw profiles:", this.$store.state.profiles);
      this.availableProfiles = this.$store.state.profiles;
	  console.debug("test: " + this.availableProfiles);
    },	
	watch: {
	    '$store.state.profiles': {
	        handler(newProfiles) {
	            console.debug('Profiles updated:', newProfiles);
	            this.availableProfiles = newProfiles;
	        },
	        immediate: true
	    }
	},

  methods: {
        toggleItemSelection(profile: Profile) {
            this.selectedProfile = profile;
        },
        handleContinueClick() {
            if (this.selectedProfile) {
                this.$store.commit('setSelectedProfile', this.selectedProfile);
                const switchProfileMessage = {
                    command: 'onSelectProfile',
                    params: this.selectedProfile                
				};
                window.ideApi.postMessage(switchProfileMessage);
            }
        }
    }
})
</script>
<style scoped lang="scss">
.profile-header {
    margin-bottom: 16px;
}

.profile-subtitle {
    font-size: 12px;
    color: #bbbbbb;
    margin-bottom: 12px;
}

.profile-list {
    display: flex;
    flex-direction: column;
}

.profile-item {
    padding: 15px;
    display: flex;
    align-items: flex-start;
    border: 1px solid #cccccc;
    border-radius: 4px;
    margin-bottom: 10px;
    cursor: pointer;
    transition: background 0.2s ease-in-out;
}

.selected {
    user-select: none;
}

.text {
    display: flex;
    flex-direction: column;
    font-size: 15px;
}
.profile-name {
    font-weight: bold;
    margin-bottom: 2px;
}
.profile-region {
    font-style: italic;
    color: #bbbbbb;
}
.profile-description {
    font-size: 12px;
    color: #bbbbbb;
}
body.jb-dark {
	.profile-name {
		color: white;
	}
    .profile-item {
        border: 1px solid white;
    }
    .selected {
        border: 1px solid #29a7ff;
    }
}

body.jb-light {
	.profile-name {
		color: black;
	}
    .profile-item {
        border: 1px solid black;
    }
    .selected {
        border: 1px solid #3574f0;
    }
}
</style>
