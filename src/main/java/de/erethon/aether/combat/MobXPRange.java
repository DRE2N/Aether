package de.erethon.aether.combat;

public record MobXPRange(int min, int max) {
    public int getRandom() {
        if (min == max) {
            return min;
        }
        return min + (int) (Math.random() * (max - min + 1));
    }
}
