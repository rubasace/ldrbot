import {describe, expect, it} from 'vitest'
import {calculatePosition, getPositionStyle} from '../leaderboard.js'

// Characterises current behaviour (spec.md:67) — AC-5's boundary table for both helpers:
// indices 0, 1, 2 and an index >= 3 (two of them, so the default branch is not read as a fluke).
describe('calculatePosition', () => {
    it.each([[0, '1st'], [1, '2nd'], [2, '3rd'], [3, '4th'], [9, '10th']])(
        'index %i -> %s', (i, out) => expect(calculatePosition(i)).toBe(out))
})

describe('getPositionStyle', () => {
    it.each([[0, 'first'], [1, 'second'], [2, 'third'], [3, 'rest'], [9, 'rest']])(
        'index %i -> %s', (i, out) => expect(getPositionStyle(i)).toBe(out))
})
