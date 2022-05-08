import * as pages from './pages/index.js'
import homepage from './pages/home.js'

export default {
    name: 'App',
    components: Object.assign({homepage}, pages),

    setup() {
        const {watchEffect, ref} = Vue;
        const page = ref(null);

        //url management
        watchEffect(() => {
            const urlpage = window.location.pathname.split("/").pop();
            if (page.value == null) {page.value = urlpage}
            if (page.value != urlpage) {const url = page.value ? page.value : './'; window.history.pushState({url: url}, '', url);                                }
            window.onpopstate = function() {page.value = window.location.pathname.split("/").pop()};
        })

        return {
            page, pages
        };
    },
    template: `
        <div id="sidebar">
            <nav>
                <button v-on:click="page = ''">Home</button>
                <template v-for="item, index in pages" key="item.name">
                    <button v-on:click="page = index">
                        {{ item.name }}
                    </button>
                </template>               
            </nav><hr>
        </div>
        <div id="content">
            <component :is="page || 'homepage'"></component>
        </div>
    `,
};