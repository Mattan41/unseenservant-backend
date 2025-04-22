import {describe, expect, it} from 'vitest'

import {mount} from '@vue/test-utils'
import HelloComponent from "@/components/HelloComponent.vue";

describe('HelloComponent', () => {
  it('renders properly', () => {
    const wrapper = mount(HelloComponent, {props: {msg: 'Welcome to Unseen Servant, the online tool for managing your tabletop RPG games.'}})
    expect(wrapper.text()).toContain('Welcome to Unseen Servant, the online tool for managing your tabletop RPG games.')
  })
})
