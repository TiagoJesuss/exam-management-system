<!-- This is just a mockup of the StatisticsView.vue component -->
<!-- It is not meant to be your final implementation -->
<!-- You should replace this file with your own implementation -->
<!-- You are free to explore the available javascript libraries for charts and graphs -->
<!-- Some popular libraries are Chart.js, D3.js, vue-chartjs, ... -->
<!-- You can also use Vuetify components to enhance the visual appeal of your statistics -->

<template>
  <v-container>
    <v-row class="mb-6">
      <v-col cols="12">
        <h1 class="text-h4 text-center mb-4">UCs Statistics Dashboard</h1>
      </v-col>
    </v-row>

    <!-- Grade Reviews Progress Section -->
    <v-row class="mb-6">
      <v-col cols="12">
        <h2 class="text-h5 mb-4">Grade Reviews Progress</h2>
        <v-row>
          <v-col
            v-for="(count, step) in reviewsProgress"
            :key="step"
            cols="12"
            sm="6"
            md="4"
            lg="3"
          >
            <v-card class="stat-card">
              <v-card-text class="text-center">
                <h3 class="text-h6">{{ step }}</h3>
                <p class="text-h4 font-weight-bold">{{ count }}</p>
                <p class="text-caption">students</p>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>
      </v-col>
    </v-row>

    <!-- Project Submissions Section -->
    <v-row class="mb-6">
      <v-col cols="12">
        <h2 class="text-h5 mb-4">Project Submissions</h2>
        <v-row>
          <v-col
            v-for="(count, step) in projectSubmissions"
            :key="step"
            cols="12"
            sm="6"
            md="4"
            lg="3"
          >
            <v-card class="stat-card">
              <v-card-text class="text-center">
                <h3 class="text-h6">{{ step }}</h3>
                <p class="text-h4 font-weight-bold">{{ count }}</p>
                <p class="text-caption">students</p>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup>
import { ref } from 'vue'

// Mock data for reviews progress
const reviewsProgress = ref({
  'Submitted': 5,
  'In Analysis': 7,
  'In review': 3,
  'Accepted': 4,
  'Rejected': 1
})

// Mock data for project submissions
const projectSubmissions = ref({
  'Submitted': 5,
  'Automatically Evaluated': 3,
  'Under Manual Review': 4,
  'Finalized': 6
})
</script>

<style scoped>
.stat-card {
  transition: transform 0.2s ease-in-out;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
}
</style>