import {describe, expect, it} from 'vitest'
import {formatDuration} from '../duration.js'

// Characterises current behaviour (spec.md:67) — AC-3's four cases, no edge-case fixes.
describe('formatDuration', () => {
    it('renders zero seconds', () => expect(formatDuration(0)).toBe('0s'))
    it('renders a sub-minute value', () => expect(formatDuration(45)).toBe('45s'))
    it('renders a minutes-only value', () => expect(formatDuration(120)).toBe('2m'))
    it('renders hours + minutes + seconds', () => expect(formatDuration(3725)).toBe('1h 2m 5s'))
})
