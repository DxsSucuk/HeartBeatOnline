<script>
	import '@fortawesome/fontawesome-free/css/all.min.css';
</script>

<div class="heart">
	<i class="fa-solid fa-heart"></i>
</div>

<style lang="postcss">
	@keyframes beat {
		0%,
		100% {
			transform: scale(1);
		}
		50% {
			transform: scale(1.1);
		}
	}

	.heart {
		font-size: 75px;
		color: red;
		animation: beat 1s infinite;
	}
</style>
