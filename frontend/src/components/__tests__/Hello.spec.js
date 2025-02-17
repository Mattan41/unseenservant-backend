import {describe, expect, it} from 'vitest'

import {mount} from '@vue/test-utils'
import Hello from "@/components/Hello.vue";

describe('Hello', () => {
  it('renders properly', () => {
    const wrapper = mount(Hello, {props: {msg: 'Welcome to Unseen Servant, the online tool for managing your tabletop RPG games.'}})
    expect(wrapper.text()).toContain('Welcome to Unseen Servant, the online tool for managing your tabletop RPG games.')
  })
})
