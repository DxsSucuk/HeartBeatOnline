import { writable } from 'svelte/store';
import type { Gambler } from './client';

export default writable<Gambler>();
