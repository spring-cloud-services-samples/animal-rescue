import React from 'react';
import {Button, Card} from 'semantic-ui-react';
import AnimalCard from "./animal-card";
import * as PropTypes from "prop-types";

export default class AnimalCards extends React.Component {

    render() {
        const cards = this.props.animals.map(animal => (
            <AnimalCard animal={animal}
                        key={animal.id}
                        username={this.props.username}/>
        ));
        return (
            <div>
                <Card.Group centered>
                    {cards}
                </Card.Group>
                {this.props.hasMore && (
                    <div style={{textAlign: 'center', margin: '20px 0'}}>
                        <Button color='green' onClick={this.props.onLoadMore}>
                            Load More
                        </Button>
                    </div>
                )}
            </div>
        );
    }
}

AnimalCards.propTypes = {
    username: PropTypes.string.isRequired,
    animals: PropTypes.arrayOf(PropTypes.shape({
        id: PropTypes.number.isRequired,
    })),
    hasMore: PropTypes.bool,
    onLoadMore: PropTypes.func,
};
