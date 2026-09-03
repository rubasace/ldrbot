// This spec asserts on layout classes (.leaderboard-row, .position, .time, .no-data). There is no
// data-testid convention in this project and the template is deliberately left untouched by this
// issue, so a future CSS rename surfaces here as a behaviour-looking failure. That is expected.
import {describe, expect, it, vi} from 'vitest'
import {flushPromises, mount} from '@vue/test-utils'
import Leaderboard from '../Leaderboard.vue'
import {formatDuration} from '../../utils/duration.js'
import {calculatePosition} from '../../utils/leaderboard.js'

const GROUP_ID = 42

// Only the subset of /api/leaderboard/{groupId} that Leaderboard.vue actually reads. The real
// GlobalLeaderboardEntry also serializes lastName and totalGames; omitting fields the component
// never reads is fixture hygiene. This is a fixture shape, not a statement about the API's shape.
// Durations 3725 / 90 / 0 / 45 cover h+m+s, m+s, the 0s edge and s-only, so the duration assertion
// is not four repeats of one format and an off-by-one row mapping cannot hide. Entry 2 carries
// username: null + firstName, exercising the `entry.username ? … : entry.firstName` fallback.
const entries = [
    {userId: 1, username: 'alice', totalPoints: 30, totalDuration: 3725},
    {userId: 2, username: null, firstName: 'Bob', totalPoints: 25, totalDuration: 90},
    {userId: 3, username: 'carol', totalPoints: 20, totalDuration: 0},
    {userId: 4, username: 'dave', totalPoints: 10, totalDuration: 45},
]

// No PrimeVue plugin is registered here, on purpose:
//   (i)  viewMode defaults to 'global' (Leaderboard.vue:23), so the primevue/select branch
//        (Leaderboard.vue:82-88) never renders in either of these two tests;
//   (ii) usePrimeVue is imported at Leaderboard.vue:3 but is never invoked anywhere in vue/src,
//        so nothing in this component reads $primevue. This is the fact that makes the bare
//        mount durable — adding a usePrimeVue() call to the component is what would break it;
//   (iii) a game-mode mount DOES need `global: {plugins: [PrimeVue]}`, and a SessionTable-style
//        mount needs that PLUS the global component registrations vue/src/main.js:30-34 performs
//        (`global: {plugins: [PrimeVue], components: {DataTable, Column}}`) — SessionTable.vue
//        never imports DataTable/Column locally, so the plugin alone still fails with
//        "Failed to resolve component: DataTable". This harness provides neither; whoever adds
//        those tests adds the option themselves.
const mountBoard = async (body) => {
    // vi.stubGlobal + unstubGlobals: true (vitest.config.js) — the stub is armed per test and
    // restored automatically, so each test gets a fresh spy. Note that unstubGlobals *restores*
    // the pre-existing fetch rather than deleting it: an un-armed mount reaches a live fetch
    // implementation, so any test added here must arm the stub itself.
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ok: true, json: async () => body}))
    const wrapper = mount(Leaderboard, {props: {group: {groupId: GROUP_ID}}})
    await flushPromises()
    return wrapper
}

describe('Leaderboard.vue', () => {
    it('renders one row per entry with position, duration and podium classes', async () => {
        const wrapper = await mountBoard({globalLeaderboard: entries, gamesLeaderboard: {}})

        const rows = wrapper.findAll('.leaderboard-row')
        expect(rows).toHaveLength(entries.length)

        // Asserted through the imported helpers, not literal strings: this keeps AC-7 a *rendering*
        // assertion (does the component wire the helper into the DOM?) and leaves the values to
        // AC-3/AC-5, so a helper change produces one clear failure instead of two.
        rows.forEach((row, i) => {
            expect(row.get('.position').text()).toBe(`${calculatePosition(i)}.`)
            expect(row.get('.time').text()).toBe(formatDuration(entries[i].totalDuration))
        })

        expect(rows[0].classes()).toContain('first')
        expect(rows[1].classes()).toContain('second')
        expect(rows[2].classes()).toContain('third')
        // Row 3 binds getPositionStyle's default branch in the rendered path, not only in AC-5.
        expect(rows[3].classes()).toContain('rest')

        expect(rows[1].get('.name').text()).toBe('Bob')

        expect(globalThis.fetch).toHaveBeenCalledWith(`/api/leaderboard/${GROUP_ID}`)
        // Exactly one leaderboard request per mount: no duplicate load, no re-entrant fetch from
        // the viewMode watcher, and nothing else in this render reached the stubbed global — this
        // populated render emits four <img src="/api/images/users/…"> (Leaderboard.vue:104).
        // On the <img> sub-resource question specifically, the durable guarantee is happy-dom's
        // own default (`enableImageFileLoading: false` in DefaultBrowserSettings), and AC-6 leg (c)
        // is the check that actually observes that path; see the impl note.
        expect(globalThis.fetch).toHaveBeenCalledTimes(1)
        expect(wrapper.findAll('img.avatar')).toHaveLength(entries.length)
    })

    it('renders the empty state', async () => {
        // {globalLeaderboard: [], gamesLeaderboard: {}} — an envelope whose global leaderboard is
        // empty, never a bare []. A bare [] leaves leaderboard.value?.globalLeaderboard undefined
        // and the rows computed throws on undefined.map — the pre-existing defect this issue does
        // not touch — and a test asserting only "zero rows" would pass vacuously against it.
        const wrapper = await mountBoard({globalLeaderboard: [], gamesLeaderboard: {}})

        // The negative assertion is paired with a positive one that can only hold if the component
        // actually rendered. Vue swallows a render-time throw into console.error.
        expect(wrapper.findAll('.leaderboard-row')).toHaveLength(0)
        expect(wrapper.get('.no-data').text()).toBe('No data found.')

        expect(globalThis.fetch).toHaveBeenCalledWith(`/api/leaderboard/${GROUP_ID}`)
        expect(globalThis.fetch).toHaveBeenCalledTimes(1)
    })
})
