<script lang="ts">
	import '../app.postcss';
	import { AppShell, AppBar } from '@skeletonlabs/skeleton';
	import { Avatar } from '@skeletonlabs/skeleton';

	// Floating UI for Popups
	import { computePosition, autoUpdate, flip, shift, offset, arrow } from '@floating-ui/dom';
	import { storePopup } from '@skeletonlabs/skeleton';
	storePopup.set({ computePosition, autoUpdate, flip, shift, offset, arrow });

	import { initializeStores, Modal } from '@skeletonlabs/skeleton';
	import { createGambler, getSelfGambler, type Gambler } from '$lib/client';
	import { onMount } from 'svelte';

	let gambler: Gambler;

	onMount(async () => {
		let user = await getSelfGambler();
		if (user == undefined) {
			user = await createGambler();
		}

		if (user == undefined) {
			gambler = user;
		}
	})

	initializeStores();
</script>
<Modal />
<!-- App Shell -->
<AppShell>
	<svelte:fragment slot="header">
		<!-- App Bar -->
		<AppBar gridColumns="grid-cols-3" slotDefault="place-self-center" slotTrail="place-content-end">
			<svelte:fragment slot="lead">
				<div />
			</svelte:fragment>
			<strong class="text-xl uppercase">HeartMyBeat</strong>
			<svelte:fragment slot="trail">
				<span class="btn variant-filled-primary">
					<span><i class="fa-solid fa-user-circle"></i></span>
					<span>{gambler !== undefined ? gambler.money : '0'}$</span>
				</span>
			</svelte:fragment>
		</AppBar>
	</svelte:fragment>
	<!-- Page Route Content -->
	<slot />
</AppShell>
