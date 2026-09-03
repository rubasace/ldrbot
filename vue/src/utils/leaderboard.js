export const getPositionStyle = (index) => {
    switch (index) {
        case 0:
            return 'first'
        case 1:
            return 'second'
        case 2:
            return 'third'
        default:
            return 'rest'
    }
}

export const calculatePosition = (index) =>
    ['1st', '2nd', '3rd'][index] || `${index + 1}th`
