<template>
  <div class="pa-4 text-center">
    <v-dialog v-model="dialog" max-width="400">
      <template v-slot:activator="{ props: activatorProps }">
        <v-btn
          class="text-none font-weight-regular"
          prepend-icon="mdi-plus"
          text="Adicionar Pessoa"
          v-bind="activatorProps"
          color="primary"
        ></v-btn>
      </template>

      <v-card prepend-icon="mdi-account" title="Nova">
        <v-card-text>
          <v-text-field label="Nome*" required v-model="newPerson.name"></v-text-field>
          <v-text-field label="Email*" type="email" required v-model="newPerson.email"></v-text-field>
          <v-text-field
            label="Palavra-passe*"
            type="password"
            required
            v-model="newPerson.password"
          ></v-text-field>

            <v-select
            :items="['Administrador', 'Funcionário', 'Professor', 'Aluno']"
            label="Categoria*"
            required
            v-model="newPerson.type"
            ></v-select>
        </v-card-text>

        <v-divider></v-divider>

        <v-card-actions>
          <v-spacer></v-spacer>

          <v-btn text="Close" variant="plain" @click="dialog = false"></v-btn>

          <v-btn
            color="primary"
            text="Save"
            variant="tonal"
            @click="
              dialog = false,
              savePerson()
            "
          ></v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type PersonDto from '@/models/PersonDto'
import RemoteService from '@/services/RemoteService'

const dialog = ref(false)

const emit = defineEmits(['person-created'])

const typeMappings = {
  'Administrador': 'ADMINISTRATOR',
  'Funcionário': 'SCHOOL_STAFF',
  'Professor': 'TEACHER',
  'Aluno': 'STUDENT'
}

const newPerson = ref<PersonDto>({
  name: '',
  email: '',
  password: '',
  type: ''
})

const savePerson = async () => {
  newPerson.value.type = typeMappings[newPerson.value.type as keyof typeof typeMappings]
  await RemoteService.createPerson(newPerson.value)
  newPerson.value = {
    name: '',
    email: '',
    password: '',
    type: ''
  }
  emit('person-created')
}
</script>
