export const formatDuration = (seconds) => {
    const h = Math.floor(seconds / 3600)
    const m = Math.floor((seconds % 3600) / 60)
    const s = seconds % 60
    return [h ? `${h}h` : '', m ? `${m}m` : '', (!h && !m) || s ? `${s}s` : '']
        .filter(Boolean)
        .join(' ')
}
